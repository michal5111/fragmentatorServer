package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Movie;
import com.michal5111.fragmentatorServer.domain.Subtitles;
import com.michal5111.fragmentatorServer.exceptions.UnknownSubtitlesTypeException;
import com.michal5111.fragmentatorServer.parsers.SubtitlesParser;
import com.michal5111.fragmentatorServer.parsers.SubtitlesParserFactory;
import com.michal5111.fragmentatorServer.repositories.LineRepository;
import com.michal5111.fragmentatorServer.repositories.MovieRepository;
import com.michal5111.fragmentatorServer.repositories.SubtitlesRepository;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class DatabaseService {

    private final MovieRepository movieRepository;

    private final LineRepository lineRepository;

    private final SubtitlesRepository subtitlesRepository;

    private final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseService(MovieRepository movieRepository, LineRepository lineRepository, SubtitlesRepository subtitlesRepository) {
        this.movieRepository = movieRepository;
        this.lineRepository = lineRepository;
        this.subtitlesRepository = subtitlesRepository;
    }

    static public Stream<Movie> findMovies() throws IOException {
        Path[] paths = new Path[]{
                Paths.get("/disks/G/Pobrane/Filmy"),
                Paths.get("/disks/G/Pobrane/Seriale"),
                Paths.get("/disks/E/Downloads/Filmy"),
                Paths.get("/disks/E/Downloads/Seriale"),
                Paths.get("/disks/G/kopia/Downloads/Seriale")
        };
        Stream<Movie>[] streams = new Stream[paths.length];
        for (int i = 0; i < paths.length; i++) {
            streams[i] = Files.walk(paths[i])
                    .filter(Files::isRegularFile)
                    .filter(Utils::endsWithSRT)
                    .map(Path::toFile)
                    .map(Utils::createMovieFromFile);

        }
        return Arrays.stream(streams)
                .flatMap(Function.identity())
                .parallel();
        //.sorted(Comparator.comparing(Movie::getFileName));
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
            } catch (FileNotFoundException | UnknownSubtitlesTypeException e) {
                emitter.error(e);
            }
            emitter.success(movie);
        });
    }

    private Mono<Boolean> isMovieExists(Movie movie) {
        return Mono.fromCallable(() -> !movieRepository
                .existsByPathAndFileNameEquals(movie.getPath(), movie.getFileName()));
    }

    public Flux<Movie> updateDatabase() throws IOException {
        return Flux.fromStream(findMovies())
                .filterWhen(this::isMovieExists)
                .doOnNext(movie -> logger.info("Adding movie: " + movie.getPath() + "/" + movie.getFileName()))
                .flatMap(this::parseSubtitles)
                .flatMap(Utils::getMovieExtension)
                .map(movieRepository::save)
                .onErrorContinue((error, object) -> logger.error(error.getMessage()));
        //.parallel();
        //.doOnComplete(this::updateIndex);
    }

    public List<Line> updateDatabase(Long id) throws FileNotFoundException, UnknownSubtitlesTypeException {
        List<Line> lineList = lineRepository.findAllBySubtitles_Movie_Id(id);
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

    public String updateIndex() {
        FullTextEntityManager fullTextEntityManager
                = Search.getFullTextEntityManager(entityManager);
        fullTextEntityManager.createIndexer().start();
        return "Success";
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
}
