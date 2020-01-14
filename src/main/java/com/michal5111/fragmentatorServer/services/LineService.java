package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.exceptions.LineNotFoundException;
import com.michal5111.fragmentatorServer.repositories.LineRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
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
}
