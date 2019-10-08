package com.michal5111.fragmentatorServer.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.repositories.LineRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Optional;

public class LineIdDeserializer extends JsonDeserializer<Line> {

    private static LineRepository lineRepository;

    @Autowired
    public LineIdDeserializer(LineRepository lineRepository) {
        LineIdDeserializer.lineRepository = lineRepository;
    }

    @Override
    public Line deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Long id = jp.readValueAs(Long.class);
        Optional<Line> optionalLine = lineRepository.findById(id);
        if (optionalLine.isEmpty()) {
            throw new IllegalStateException();
        }
        return optionalLine.get();
    }
}
