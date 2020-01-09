package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.Controllers.RestController;
import com.michal5111.fragmentatorServer.domain.FragmentRequest;
import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.exceptions.FragmentRequestNotFoundException;
import com.michal5111.fragmentatorServer.exceptions.InvalidFFMPEGPropertiesException;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.exceptions.SubtitlesNotFoundException;
import com.michal5111.fragmentatorServer.repositories.FragmentRequestRepository;
import com.michal5111.fragmentatorServer.repositories.LineEditRepository;
import com.michal5111.fragmentatorServer.repositories.LineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class FragmentRequestService {

    private final FragmentRequestRepository fragmentRequestRepository;

    private final LineEditRepository lineEditRepository;

    private final LineRepository lineRepository;

    private final ConverterService converterService;

    private final Logger logger = LoggerFactory.getLogger(RestController.class);

    public FragmentRequestService(FragmentRequestRepository fragmentRequestRepository, LineEditRepository lineEditRepository, LineRepository lineRepository, ConverterService converterService) {
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

    public Flux<ConverterService.ConversionStatus> get(Long id) throws InterruptedException, MovieNotFoundException, IOException, FragmentRequestNotFoundException, SubtitlesNotFoundException, InvalidFFMPEGPropertiesException {
        Optional<FragmentRequest> optionalFragmentRequest = fragmentRequestRepository.findById(id);
        if (optionalFragmentRequest.isEmpty()) {
            throw new FragmentRequestNotFoundException("Fragment Request Not Found!");
        }
        FragmentRequest fragmentRequest = optionalFragmentRequest.get();
        List<Line> lines = lineRepository.findAllByIdBetween(
                fragmentRequest.getStartLine().getId(),
                fragmentRequest.getStopLine().getId()
        );
        logger.info("Request for: " + fragmentRequest.getMovie().getFileName() + " "
                + fragmentRequest.getStartLine().getTextLines());
        return converterService.convertFragment(fragmentRequest, lines);
    }

    public String progress(String progress) {
        logger.debug(progress);
        return progress;
    }
}
