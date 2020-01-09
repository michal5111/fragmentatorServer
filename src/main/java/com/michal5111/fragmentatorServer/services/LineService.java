package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.exceptions.InvalidFFMPEGPropertiesException;
import com.michal5111.fragmentatorServer.exceptions.LineNotFoundException;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.repositories.LineRepository;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class LineService {

    private final LineRepository lineRepository;

    private final ConverterService converterService;

    public LineService(LineRepository lineRepository, ConverterService converterService) {
        this.lineRepository = lineRepository;
        this.converterService = converterService;
    }

    public Map<String, String> getSnapshot(Long id, HttpServletRequest request) throws LineNotFoundException, InterruptedException, MovieNotFoundException, IOException, InvalidFFMPEGPropertiesException {
        Optional<Line> optionalLine = lineRepository.findById(id);
        if (optionalLine.isEmpty()) {
            throw new LineNotFoundException("Line Not Found!");
        }
        Line line = optionalLine.get();
        Map<String, String> map = new HashMap<>();
        map.put("url", "http://" + request.getServerName() + ":" + request.getServerPort() + "/fragmentatorServer/snapshots/" + converterService.getSnapshot(line));
        return map;
    }
}
