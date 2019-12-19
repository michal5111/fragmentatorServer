package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Movie;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.repositories.MovieRepository;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
public class DatabaseService {

    private final MovieRepository movieRepository;

    private final Logger logger =  LoggerFactory.getLogger(DatabaseService.class);

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<Movie> updateDatabase() throws IOException, InterruptedException {
        List<Movie> addedMovies = new LinkedList<>();
        Utils.findMovies().forEach(movie -> {
            Optional<Movie> optionalMovie = movieRepository
                    .findByPathAndFileNameEquals(movie.getPath(),movie.getFileName());
            if (optionalMovie.isPresent()) {
                return;
            }
            logger.info("Adding movie: " + movie.getPath()+"/"+movie.getFileName());
            try {
                movie.getSubtitles().parse();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            movie.getSubtitles().getLines().forEach(Line::parseTime);

            addedMovies.add(movie);
            movieRepository.save(movie);
        });
        updateIndex();
        return addedMovies;
    }

    public String updateIndex() throws InterruptedException {
        FullTextEntityManager fullTextEntityManager
                = Search.getFullTextEntityManager(entityManager);
        fullTextEntityManager.createIndexer().startAndWait();
        return "Success";
    }

    public List<Movie> cleanDatabase() {
        List<Movie> deletedMovies = new LinkedList<>();
        List<Movie> movies = movieRepository.findAll();
        movies.forEach(movie -> {
            try {
                if (movie == null) {
                    logger.debug("Movie is null");
                    return;
                }
                logger.debug(movie.getPath());
                logger.debug(movie.getFileName());;
                movie.setExtension(Utils.getMovieExtension(Path.of(movie.getPath()), movie.getFileName()));
                logger.debug(movie.getExtension());
                File movieFile = new File(movie.getPath(), movie.getFileName()+movie.getExtension());
                File srtFile = new File(movie.getPath(), movie.getSubtitles().getFilename());
                if (!movieFile.exists() || !srtFile.exists()) {
                    movieRepository.delete(movie);
                    deletedMovies.add(movie);
                }
            } catch (IOException | MovieNotFoundException e) {
                logger.debug(e.getMessage());
            }
        });
        return deletedMovies;
    }
}
