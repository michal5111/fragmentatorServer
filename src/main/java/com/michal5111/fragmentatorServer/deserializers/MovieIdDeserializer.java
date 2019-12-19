package com.michal5111.fragmentatorServer.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.michal5111.fragmentatorServer.domain.Movie;
import com.michal5111.fragmentatorServer.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class MovieIdDeserializer extends JsonDeserializer<Movie> {

    private static MovieRepository movieRepository;

    @Autowired
    public MovieIdDeserializer(MovieRepository movieRepository) {
        MovieIdDeserializer.movieRepository = movieRepository;
    }

    @Override
    public Movie deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        Long id = jp.readValueAs(Long.class);
        Optional<Movie> optionalMovie = movieRepository.findById(id);
        if (optionalMovie.isEmpty()) {
            throw new IllegalStateException();
        }
        return optionalMovie.get();
    }
}
