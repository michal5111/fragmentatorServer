package com.michal5111.fragmentator_server.controllers;

import com.michal5111.fragmentator_server.domain.FragmentRequest;
import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.LineEdit;
import com.michal5111.fragmentator_server.domain.Movie;
import com.michal5111.fragmentator_server.dto.FragmentRequestDTO;
import com.michal5111.fragmentator_server.dto.LineEditDTO;
import com.michal5111.fragmentator_server.exceptions.LineNotFoundException;
import com.michal5111.fragmentator_server.exceptions.MovieNotFoundException;
import com.michal5111.fragmentator_server.exceptions.UnknownSubtitlesTypeException;
import com.michal5111.fragmentator_server.repositories.LineRepository;
import com.michal5111.fragmentator_server.repositories.MovieRepository;
import com.michal5111.fragmentator_server.services.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
@org.springframework.web.bind.annotation.RestController
@RequestMapping("api")
public class RestController {

    private final MovieRepository movieRepository;

    private final LineRepository lineRepository;

    private final DatabaseService databaseService;

    private final FragmentRequestService fragmentRequestService;

    private final LineService lineService;

    private final SearchService searchService;

    private final YouTubeDlService youTubeDlService;

    private final ModelMapper modelMapper = new ModelMapper();

    public RestController(MovieRepository movieRepository,
                          LineRepository lineRepository,
                          DatabaseService databaseService,
                          FragmentRequestService fragmentRequestService,
                          LineService lineService,
                          SearchService searchService,
                          YouTubeDlService youTubeDlService) {
        this.movieRepository = movieRepository;
        this.lineRepository = lineRepository;
        this.databaseService = databaseService;
        this.fragmentRequestService = fragmentRequestService;
        this.lineService = lineService;
        this.searchService = searchService;
        this.youTubeDlService = youTubeDlService;
    }

    @PostMapping("/fragmentRequest")
    @ResponseStatus(HttpStatus.CREATED)
    public FragmentRequest createFragmentRequest(@RequestBody FragmentRequestDTO fragmentRequestDTO)
            throws MovieNotFoundException, LineNotFoundException {
        log.info("Fragment request: {}", fragmentRequestDTO);
        FragmentRequest fragmentRequest = convertToEntity(fragmentRequestDTO);
        return fragmentRequestService.create(fragmentRequest);
    }

    @GetMapping(path = "/fragmentRequest/{id}", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<ConverterService.ConversionStatus> requestFragment(@PathVariable("id") Long id) {
        return fragmentRequestService.get(id)
                .subscribeOn(Schedulers.elastic());
    }

    @GetMapping(value = "updateDatabase", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<Movie> updateDatabase() throws IOException {
        return databaseService.updateDatabase()
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("updateDatabase/{id}")
    public List<Line> updateSubtitles(@PathVariable("id") Long id) throws UnknownSubtitlesTypeException, ParseException {
        return databaseService.updateDatabase(id);
    }

    @DeleteMapping("/updateDatabase")
    public List<Movie> cleanDatabase() {
        return databaseService.cleanDatabase();
    }

    @GetMapping("/lineHints")
    public List<Line> getLinesSQL(@RequestParam("phrase") String phrase) {
        return lineRepository.findText2(phrase);
    }

    @GetMapping("/movieHints")
    public List<Movie> getTitleHints(@RequestParam("title") String title) {
        return movieRepository.findTitleHints(title);
    }

    @GetMapping("/searchMovie")
    public List<Movie> getMovieByTitleSQL(@RequestParam("title") String title) {
        return movieRepository.findMovieByFileNameContainingIgnoreCase(title);
    }

    @GetMapping("/lines")
    public List<Line> getLines(@RequestParam("movieId") Long movieId) {
        return lineRepository.findAllBySubtitlesMovieId(movieId);
    }

    @PutMapping(value = "/index", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Float> updateIndex2() {
        return databaseService.updateIndex2();
    }


    @GetMapping("/searchPhrase")
    public Page<Line> searchLineIndexed2(
            @RequestParam("phrase") String phrase,
            @RequestParam(name = "title", required = false) String title,
            Pageable pageable) {
        return searchService.search(phrase, title, pageable);
    }

    @GetMapping("/lineSnapshot")
    public Mono<ResponseEntity<Void>> getLineSnapshot(
            @RequestParam("lineId") Long lineId,
            HttpServletRequest request
    ) throws LineNotFoundException {
        return lineService.getSnapshot(lineId, request)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/progress")
    public void handleRequest(InputStream dataStream) throws IOException {
        int i;
        char c;
        StringBuilder stringBuilder = new StringBuilder();
        while ((i = dataStream.read()) != -1) {
            c = (char) i;
            if (c != '\n') {
                stringBuilder.append(c);
            } else {
                log.debug("Progress: {}", stringBuilder);
                stringBuilder = new StringBuilder();
            }
        }
    }

    @PutMapping(value = "/youtube", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<String> getYoutubeInfo(@RequestParam String url) {
        return youTubeDlService.getInfo(url)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(value = "/youtube", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<YouTubeDlService.DownloadStatus> getYoutubeVideo(@RequestParam String url) {
        return youTubeDlService.download(url)
                .subscribeOn(Schedulers.boundedElastic());
    }

    public FragmentRequest convertToEntity(FragmentRequestDTO fragmentRequestDTO) throws LineNotFoundException, MovieNotFoundException {
        Optional<Movie> optionalMovie = movieRepository.findById(fragmentRequestDTO.getMovieId());
        Optional<Line> optionalStartLine = lineRepository.findById(fragmentRequestDTO.getStartLineId());
        Optional<Line> optionalStopLine = lineRepository.findById(fragmentRequestDTO.getStopLineId());
        if (optionalMovie.isEmpty()) {
            throw new MovieNotFoundException("Movie not found!");
        }
        if (optionalStartLine.isEmpty() || optionalStopLine.isEmpty()) {
            throw new LineNotFoundException("Line not found!");
        }
        Movie movie = optionalMovie.get();
        Line startLine = optionalStartLine.get();
        Line stopLine = optionalStopLine.get();
        List<LineEdit> lineEdits = new LinkedList<>();
        FragmentRequest fragmentRequest = modelMapper.map(fragmentRequestDTO, FragmentRequest.class);
        for (LineEditDTO lineEditDTO : fragmentRequestDTO.getLineEdits()) {
            LineEdit lineEdit = convertToEntity(lineEditDTO);
            lineEdit.setFragmentRequest(fragmentRequest);
            lineEdits.add(lineEdit);
        }
        fragmentRequest.setLineEdits(lineEdits);
        fragmentRequest.setMovie(movie);
        fragmentRequest.setStartLine(startLine);
        fragmentRequest.setStopLine(stopLine);
        return fragmentRequest;
    }

    public LineEdit convertToEntity(LineEditDTO lineEditDTO) throws LineNotFoundException {
        Optional<Line> optionalLine = lineRepository.findById(lineEditDTO.getLineId());
        if (optionalLine.isEmpty()) {
            throw new LineNotFoundException("Line not found!");
        }
        Line line = optionalLine.get();
        LineEdit lineEdit = modelMapper.map(lineEditDTO, LineEdit.class);
        lineEdit.setLine(line);
        return lineEdit;
    }
}
