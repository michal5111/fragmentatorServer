package com.michal5111.fragmentator_server.controllers;

import com.michal5111.fragmentator_server.domain.FragmentRequest;
import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Movie;
import com.michal5111.fragmentator_server.exceptions.LineNotFoundException;
import com.michal5111.fragmentator_server.exceptions.UnknownSubtitlesTypeException;
import com.michal5111.fragmentator_server.repositories.LineRepository;
import com.michal5111.fragmentator_server.repositories.MovieRepository;
import com.michal5111.fragmentator_server.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("api")
public class RestController {

    private final MovieRepository movieRepository;

    private final LineRepository lineRepository;

    private final DatabaseService databaseService;

    private final FragmentRequestService fragmentRequestService;

    private final LineService lineService;

    private final SearchService searchService;

    private final Logger logger = LoggerFactory.getLogger(RestController.class);

    public RestController(MovieRepository movieRepository,
                          LineRepository lineRepository,
                          DatabaseService databaseService,
                          FragmentRequestService fragmentRequestService,
                          LineService lineService,
                          SearchService searchService) {
        this.movieRepository = movieRepository;
        this.lineRepository = lineRepository;
        this.databaseService = databaseService;
        this.fragmentRequestService = fragmentRequestService;
        this.lineService = lineService;
        this.searchService = searchService;
    }

    @PostMapping("/fragmentRequest")
    @ResponseStatus(HttpStatus.CREATED)
    public FragmentRequest createFragmentRequest(@RequestBody FragmentRequest fragmentRequest) {
        return fragmentRequestService.create(fragmentRequest);
    }

    @GetMapping(path = "/fragmentRequest/{id}", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<ConverterService.ConversionStatus> requestFragment(@PathVariable("id") Long id) {
        return fragmentRequestService.get(id)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(value = "updateDatabase", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<Movie> updateDatabase() throws IOException {
        return databaseService.updateDatabase()
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("updateDatabase/{id}")
    public List<Line> updateSubtitles(@PathVariable("id") Long id) throws IOException, UnknownSubtitlesTypeException {
        return databaseService.updateDatabase(id);
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

    @GetMapping("/getLines")
    public List<Line> getLines(@RequestParam("movieId") Long movieId) {
        return lineRepository.findAllBySubtitles_Movie_Id(movieId);
    }

    @GetMapping("/getFilteredLines")
    public List<Line> getFilteredLines(@RequestParam("subtitlesId") Long movieId,
                                       @RequestParam("phrase") String phrase) {
        return lineRepository.findFilteredLines(movieId, phrase);
    }

    @GetMapping(value = "/updateIndex", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String updateIndex() {
        return databaseService.updateIndex();
    }

    @GetMapping("/cleanDatabase")
    public List<Movie> cleanDatabase() {
        return databaseService.cleanDatabase();
    }


    @GetMapping("/searchPhrase")
    public Page<Line> searchLineIndexed2(
            @RequestParam("phrase") String phrase, @RequestParam(name = "title", required = false) String title, Pageable pageable) {
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
                logger.debug("Progress: {}", stringBuilder);
                stringBuilder = new StringBuilder();
            }
        }
    }
}
