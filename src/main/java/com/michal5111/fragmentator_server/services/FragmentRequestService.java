package com.michal5111.fragmentator_server.services;

import com.michal5111.fragmentator_server.domain.FragmentRequest;
import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.exceptions.FragmentRequestNotFoundException;
import com.michal5111.fragmentator_server.exceptions.LineNotFoundException;
import com.michal5111.fragmentator_server.repositories.FragmentRequestRepository;
import com.michal5111.fragmentator_server.repositories.LineRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FragmentRequestService {

    private final FragmentRequestRepository fragmentRequestRepository;

    private final LineRepository lineRepository;

    private final ConverterService converterService;

    public FragmentRequestService(FragmentRequestRepository fragmentRequestRepository,
                                  LineRepository lineRepository,
                                  ConverterService converterService) {
        this.fragmentRequestRepository = fragmentRequestRepository;
        this.lineRepository = lineRepository;
        this.converterService = converterService;
    }

    public FragmentRequest create(FragmentRequest fragmentRequest) {
        return fragmentRequestRepository.save(fragmentRequest);
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
        log.info("Request for: {} {}", fragmentRequest.getMovie().getFileName(), fragmentRequest.getStartLine().getTextLines());
        return converterService.convertFragment(fragmentRequest);
    }
}
