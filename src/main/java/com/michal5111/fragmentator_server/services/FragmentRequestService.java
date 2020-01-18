package com.michal5111.fragmentator_server.services;

import com.michal5111.fragmentator_server.domain.FragmentRequest;
import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.exceptions.FragmentRequestNotFoundException;
import com.michal5111.fragmentator_server.exceptions.LineNotFoundException;
import com.michal5111.fragmentator_server.repositories.FragmentRequestRepository;
import com.michal5111.fragmentator_server.repositories.LineEditRepository;
import com.michal5111.fragmentator_server.repositories.LineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Service
public class FragmentRequestService {

    private final FragmentRequestRepository fragmentRequestRepository;

    private final LineEditRepository lineEditRepository;

    private final LineRepository lineRepository;

    private final ConverterService converterService;

    private final Logger logger = LoggerFactory.getLogger(FragmentRequestService.class);

    public FragmentRequestService(FragmentRequestRepository fragmentRequestRepository,
                                  LineEditRepository lineEditRepository,
                                  LineRepository lineRepository,
                                  ConverterService converterService) {
        this.fragmentRequestRepository = fragmentRequestRepository;
        this.lineEditRepository = lineEditRepository;
        this.lineRepository = lineRepository;
        this.converterService = converterService;
    }

    public FragmentRequest create(FragmentRequest fragmentRequest) {
        fragmentRequest = fragmentRequestRepository.save(fragmentRequest);
        FragmentRequest finalFragmentRequest = fragmentRequest;
        fragmentRequest.getLineEdits().forEach(lineEdit -> lineEdit.setFragmentRequest(finalFragmentRequest));
        lineEditRepository.saveAll(fragmentRequest.getLineEdits());
        return fragmentRequest;
    }

    public Flux<ConverterService.ConversionStatus> get(Long id) {
        Optional<FragmentRequest> optionalFragmentRequest = fragmentRequestRepository.findById(id);
        if (optionalFragmentRequest.isEmpty()) {
            return Flux.error(new FragmentRequestNotFoundException("Fragment Request Not Found!"));
        }
        FragmentRequest fragmentRequest = optionalFragmentRequest.get();
        List<Line> lines = lineRepository.findAllByIdBetween(
                fragmentRequest.getStartLine().getId(),
                fragmentRequest.getStopLine().getId()
        );
        if (lines.isEmpty()) {
            return Flux.error(new LineNotFoundException("Lines not found!"));
        }
        fragmentRequest.setLines(lines);
        logger.info("Request for: {} {}", fragmentRequest.getMovie().getFileName(), fragmentRequest.getStartLine().getTextLines());
        return converterService.convertFragment(fragmentRequest);
    }

    public String progress(String progress) {
        logger.debug(progress);
        return progress;
    }
}
