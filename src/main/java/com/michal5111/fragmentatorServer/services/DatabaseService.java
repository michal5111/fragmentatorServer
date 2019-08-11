package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.repositories.MovieRepository;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Service
public class DatabaseService {

    private final MovieRepository movieRepository;

    public DatabaseService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<Movie> updateDatabase() throws IOException {
        List<Movie> addedMovies = new LinkedList<>();
        Utils.findMovies().forEach(movie -> {
            Optional<Movie> optionalMovie = movieRepository
                    .findByPathAndFileNameEquals(movie.getPath(),movie.getFileName());
            if (optionalMovie.isPresent()) {
                return;
            }
            System.out.println("Adding movie: " + movie.getPath()+"/"+movie.getFileName());
            try {
                movie.getSubtitles().parse();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            movie.getSubtitles().getLines().forEach(Line::parseTime);

            addedMovies.add(movie);
            movieRepository.save(movie);
        });
        return addedMovies;
    }
}
