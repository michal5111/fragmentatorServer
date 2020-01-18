package com.michal5111.fragmentator_server.services;

import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Movie;
import com.michal5111.fragmentator_server.domain.Subtitles;
import com.michal5111.fragmentator_server.exceptions.UnknownSubtitlesTypeException;
import com.michal5111.fragmentator_server.parsers.SubtitlesParser;
import com.michal5111.fragmentator_server.parsers.SubtitlesParserFactory;
import com.michal5111.fragmentator_server.repositories.LineRepository;
import com.michal5111.fragmentator_server.repositories.MovieRepository;
import com.michal5111.fragmentator_server.repositories.SubtitlesRepository;
import com.michal5111.fragmentator_server.utils.Properties;
import com.michal5111.fragmentator_server.utils.Utils;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DatabaseService {

    private final MovieRepository movieRepository;

    private final LineRepository lineRepository;

    private final SubtitlesRepository subtitlesRepository;

    private final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private final Properties properties;

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseService(MovieRepository movieRepository, LineRepository lineRepository, SubtitlesRepository subtitlesRepository, Properties properties) {
        this.movieRepository = movieRepository;
        this.lineRepository = lineRepository;
        this.subtitlesRepository = subtitlesRepository;
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
            SubtitlesParserFactory subtitlesParserFactory = new SubtitlesParserFactory(subtitles.getFilename());
            SubtitlesParser subtitlesParser;
            try {
                subtitlesParser = subtitlesParserFactory.create();
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
                .doOnNext(movie -> logger.debug("Found movie          {}/{}", movie.getPath(), movie.getFileName()))
                .flatMap(this::parseSubtitles)
                .doOnNext(movie -> logger.debug("Parsed subtitles for {}/{}", movie.getPath(), movie.getFileName()))
                .flatMap(Utils::getMovieExtension)
                .map(movieRepository::save)
                .doOnNext(movie -> logger.debug("Adding movie:        {}/{}", movie.getPath(), movie.getFileName()))
                .onErrorContinue(this::updateDatabaseExceptionHandler);
    }

    public List<Line> updateDatabase(Long id) throws UnknownSubtitlesTypeException, ParseException {
        List<Line> lineList = lineRepository.findAllBySubtitlesMovieId(id);
        lineRepository.deleteAll(lineList);
        Optional<Movie> optionalMovie = movieRepository.findById(id);
        List<Line> lines = new LinkedList<>();
        if (optionalMovie.isPresent()) {
            Movie movie = optionalMovie.get();
            Subtitles subtitles = movie.getSubtitles();
            SubtitlesParserFactory subtitlesParserFactory = new SubtitlesParserFactory(subtitles.getFilename());
            SubtitlesParser subtitlesParser = subtitlesParserFactory.create();
            lines = subtitlesParser.parse(subtitles);
            subtitles.setLines(lines);
            subtitlesRepository.save(subtitles);
        }
        return lines;
    }

    public ResponseEntity<Void> updateIndex() {
        FullTextEntityManager fullTextEntityManager
                = Search.getFullTextEntityManager(entityManager);
        fullTextEntityManager.createIndexer().start();
        return ResponseEntity.ok().build();
    }

    public Flux<Boolean> updateIndex2() {
        return Flux.create(emitter -> {
            FullTextEntityManager fullTextEntityManager
                    = Search.getFullTextEntityManager(entityManager);
            Future<?> future = fullTextEntityManager.createIndexer().start();
            while (!future.isDone()) {
                emitter.next(false);
            }
            emitter.next(true);
            emitter.complete();
        });
    }

    public List<Movie> cleanDatabase() {
        List<Movie> deletedMovies = new LinkedList<>();
        List<Movie> movies = movieRepository.findAll();
        movies.forEach(movie -> {
            if (movie == null) {
                logger.debug("Movie is null");
                return;
            }
            logger.debug(movie.getPath());
            logger.debug(movie.getFileName());
            logger.debug(movie.getExtension());
            File movieFile = new File(movie.getPath(), movie.getFileName() + movie.getExtension());
            File srtFile = movie.getSubtitles().getSubtitleFile();
            if (!movieFile.exists() || !srtFile.exists()) {
                movieRepository.delete(movie);
                deletedMovies.add(movie);
            }
        });
        return deletedMovies;
    }

    private void updateDatabaseExceptionHandler(Throwable throwable, Object object) {
        logger.error("Error in adding movie {} {}", throwable.getClass().getSimpleName(), throwable.getMessage());
    }
}
