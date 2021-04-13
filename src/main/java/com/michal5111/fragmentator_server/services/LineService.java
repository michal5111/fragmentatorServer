package com.michal5111.fragmentator_server.services;

import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.exceptions.LineNotFoundException;
import com.michal5111.fragmentator_server.repositories.LineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@Service
public class LineService {

    private final LineRepository lineRepository;

    private final ConverterService converterService;

    public LineService(LineRepository lineRepository, ConverterService converterService) {
        this.lineRepository = lineRepository;
        this.converterService = converterService;
    }

    public Mono<ResponseEntity<Void>> getSnapshot(Long id, HttpServletRequest request) throws LineNotFoundException {
        Optional<Line> optionalLine = lineRepository.findById(id);
        if (optionalLine.isEmpty()) {
            throw new LineNotFoundException("Line Not Found!");
        }
        Line line = optionalLine.get();
        return converterService.getSnapshot(line)
                .map(file -> ResponseEntity
                        .status(HttpStatus.MOVED_PERMANENTLY)
                        .header(HttpHeaders.LOCATION,
                                "http://" + request.getServerName() + ":" + request.getServerPort() + "/fragmentatorServer/snapshots/" + file.getName())
                        .build());
    }

    public Page<Line> fullTextSearchPhrase(String phrase, String filter, Pageable pageable) {
        return lineRepository.fullTextSearchPhrase(phrase, filter, pageable);
    }

    public List<Line> findAllBySubtitlesMovieId(Long movieId) {
        return lineRepository.findAllBySubtitlesMovieIdOrderByNumber(movieId);
    }

    public List<Line> findText2(String phrase) {
        return lineRepository.findText2(phrase);
    }

    public Optional<Line> findById(Long lineId) {
        return lineRepository.findById(lineId);
    }
}
