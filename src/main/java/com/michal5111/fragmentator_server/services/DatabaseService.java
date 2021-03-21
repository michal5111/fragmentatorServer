package com.michal5111.fragmentator_server.services;

import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Movie;
import com.michal5111.fragmentator_server.domain.Subtitles;
import com.michal5111.fragmentator_server.exceptions.UnknownSubtitlesTypeException;
import com.michal5111.fragmentator_server.parsers.SubtitlesParser;
import com.michal5111.fragmentator_server.parsers.SubtitlesParserFactory;
import com.michal5111.fragmentator_server.repositories.FragmentRequestRepository;
import com.michal5111.fragmentator_server.repositories.LineRepository;
import com.michal5111.fragmentator_server.repositories.MovieRepository;
import com.michal5111.fragmentator_server.repositories.SubtitlesRepository;
import com.michal5111.fragmentator_server.utils.Properties;
import com.michal5111.fragmentator_server.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DatabaseService {

    private final MovieRepository movieRepository;

    private final LineRepository lineRepository;

    private final SubtitlesRepository subtitlesRepository;

    private final FragmentRequestRepository fragmentRequestRepository;

    private final Properties properties;

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseService(MovieRepository movieRepository, LineRepository lineRepository, SubtitlesRepository subtitlesRepository, FragmentRequestRepository fragmentRequestRepository, Properties properties) {
        this.movieRepository = movieRepository;
        this.lineRepository = lineRepository;
        this.subtitlesRepository = subtitlesRepository;
        this.fragmentRequestRepository = fragmentRequestRepository;
        this.properties = properties;
    }

    public Stream<Movie> findMovies() throws IOException {
        List<Path> paths = Arrays
                .stream(properties.getSearchDirectories())
                .map(Paths::get)
                .collect(Collectors.toUnmodifiableList());
        Stream<Movie>[] streams = new Stream[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            streams[i] = Files.walk(paths.get(i))
                    .filter(Files::isRegularFile)
                    .filter(Utils::endsWithSRT)
                    .map(Path::toFile)
                    .map(Utils::createMovieFromFile);
        }
        return Arrays.stream(streams)
                .flatMap(Function.identity())
                .parallel();
    }

    private Mono<Movie> parseSubtitles(Movie movie) {
        return Mono.create(emitter -> {
            Subtitles subtitles = movie.getSubtitles();
            SubtitlesParserFactory subtitlesParserFactory = new SubtitlesParserFactory();
            SubtitlesParser subtitlesParser;
            try {
                subtitlesParser = subtitlesParserFactory.create(subtitles.getFilename());
                List<Line> lineList = subtitlesParser.parse(subtitles);
                subtitles.setLines(lineList);
                emitter.success(movie);
            } catch (UnknownSubtitlesTypeException | ParseException e) {
                emitter.error(e);
            }
        });
    }

    private Mono<Boolean> movieExists(Movie movie) {
        return Mono.fromCallable(() -> !movieRepository
                .existsByFileNameEquals(movie.getFileName()));
    }

    public Flux<Movie> updateDatabase() throws IOException {
        return Flux.fromStream(findMovies())
                .filterWhen(this::movieExists)
                .doOnNext(movie -> log.debug("Found movie          {}/{}", movie.getPath(), movie.getFileName()))
                .flatMap(this::parseSubtitles)
                .doOnNext(movie -> log.debug("Parsed subtitles for {}/{}", movie.getPath(), movie.getFileName()))
                .flatMap(Utils::getMovieExtension)
                .doOnNext(movie -> log.debug("Found movie ext:     {}", movie.getExtension()))
                .map(movieRepository::save)
                .doOnNext(movie -> log.debug("Adding movie:        {}/{}", movie.getPath(), movie.getFileName()))
                .onErrorContinue(this::updateDatabaseExceptionHandler);
    }

    @Transactional
    public List<Line> updateDatabase(Long id) throws UnknownSubtitlesTypeException, ParseException {
        List<Line> lineList = lineRepository.findAllBySubtitlesMovieId(id);
        lineRepository.deleteAll(lineList);
        Optional<Movie> optionalMovie = movieRepository.findById(id);
        List<Line> lines = new LinkedList<>();
        if (optionalMovie.isPresent()) {
            Movie movie = optionalMovie.get();
            Subtitles subtitles = movie.getSubtitles();
            SubtitlesParserFactory subtitlesParserFactory = new SubtitlesParserFactory();
            SubtitlesParser subtitlesParser = subtitlesParserFactory.create(subtitles.getFilename());
            lines = subtitlesParser.parse(subtitles);
            subtitles.setLines(lines);
            subtitlesRepository.save(subtitles);
        }
        return lines;
    }

    public ResponseEntity<Void> updateIndex() {
        FullTextEntityManager fullTextEntityManager
                = Search.getFullTextEntityManager(entityManager);
        fullTextEntityManager.createIndexer()
                .optimizeOnFinish(true)
                .start();
        return ResponseEntity.ok().build();
    }

    public Flux<Float> updateIndex2() {
        return Flux.create(emitter -> {
            FullTextEntityManager fullTextEntityManager
                    = Search.getFullTextEntityManager(entityManager);
            fullTextEntityManager
                    .createIndexer()
                    .progressMonitor(new MassIndexerProgressMonitor() {
                        private static final int LOG_AFTER_NUMBER_OF_DOCUMENTS = 300;
                        private final AtomicLong documentsDoneCounter = new AtomicLong();
                        private final LongAdder totalCounter = new LongAdder();
                        private volatile long startTime;

                        @Override
                        public void documentsBuilt(int i) {
                            //not used
                        }

                        @Override
                        public void entitiesLoaded(int i) {
                            //not used
                        }

                        @Override
                        public void addToTotalCount(long count) {
                            totalCounter.add(count);
                        }

                        @Override
                        public void indexingCompleted() {
                            log.debug("Indexing complete");
                            emitter.complete();
                        }

                        @Override
                        public void documentsAdded(long increment) {
                            long current = documentsDoneCounter.addAndGet(increment);
                            if (current == increment) {
                                startTime = System.nanoTime();
                            }
                            if (current % LOG_AFTER_NUMBER_OF_DOCUMENTS == 0) {
                                printStatusMessage(startTime, totalCounter.longValue(), current);
                            }
                        }

                        protected void printStatusMessage(long startTime, long totalTodoCount, long doneCount) {
                            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                            //log.indexingDocumentsCompleted( doneCount, elapsedMs );
                            float estimateSpeed = doneCount * 1000f / elapsedMs;
                            float estimatePercentileComplete = doneCount * 100f / totalTodoCount;
                            //log.indexingSpeed( estimateSpeed, estimatePercentileComplete );
                            log.info("Indexing progress: {}", estimatePercentileComplete);
                            emitter.next(estimatePercentileComplete);
                        }
                    })
                    .start();
        });
    }

    @Transactional
    public List<Movie> cleanDatabase() {
        List<Movie> deletedMovies = new LinkedList<>();
        List<Movie> movies = movieRepository.findAll();
        movies.forEach(movie -> {
            if (movie == null) {
                log.debug("Movie is null");
                return;
            }
            log.debug(movie.getPath());
            log.debug(movie.getFileName());
            log.debug(movie.getExtension());
            File srtFile = movie.getSubtitles().getSubtitleFile();
            if (!movie.getMovieFile().exists() || !srtFile.exists()) {
                fragmentRequestRepository.deleteAllByMovieId(movie.getId());
                movieRepository.delete(movie);
                deletedMovies.add(movie);
            }
        });
        return deletedMovies;
    }

    public Flux<Movie> cleanDatabaseFlux() {
        return findAllMovies()
                .filterWhen(this::movieFilesNotExists)
                .doOnNext(movie -> {
                    fragmentRequestRepository.deleteAllByMovieId(movie.getId());
                    movieRepository.delete(movie);
                });
    }

    private Flux<Movie> findAllMovies() {
        return Flux.fromStream(movieRepository.findAll().stream());
    }

    private Mono<Boolean> movieFilesNotExists(Movie movie) {
        return Mono.fromCallable(() -> !movie.getMovieFile().exists() || !movie.getSubtitles().getSubtitleFile().exists());
    }

    private void updateDatabaseExceptionHandler(Throwable throwable, Object object) {
        log.error("Error in adding movie {} {}", throwable.getClass().getSimpleName(), throwable.getMessage());
    }
}
