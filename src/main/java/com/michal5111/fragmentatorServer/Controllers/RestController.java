package com.michal5111.fragmentatorServer.Controllers;

import com.michal5111.fragmentatorServer.domain.FragmentRequest;
import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Movie;
import com.michal5111.fragmentatorServer.exceptions.FragmentRequestNotFoundException;
import com.michal5111.fragmentatorServer.exceptions.LineNotFoundException;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.exceptions.UnknownSubtitlesTypeException;
import com.michal5111.fragmentatorServer.repositories.LineRepository;
import com.michal5111.fragmentatorServer.repositories.MovieRepository;
import com.michal5111.fragmentatorServer.services.DatabaseService;
import com.michal5111.fragmentatorServer.services.FragmentRequestService;
import com.michal5111.fragmentatorServer.services.LineService;
import com.michal5111.fragmentatorServer.services.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("api")
public class RestController {

    private final MovieRepository movieRepository;

    private final LineRepository lineRepository;

    private final DatabaseService databaseService;

    private final FragmentRequestService fragmentRequestService;

    private final LineService lineService;

    private final SearchService searchService;

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

    @GetMapping(path = "/fragmentRequest/{id}")
    public Flux<ServerSentEvent<String>> requestFragment(
            @PathVariable("id") Long id
    ) throws FragmentRequestNotFoundException, InterruptedException, MovieNotFoundException, IOException {
        return fragmentRequestService.get(id);
    }

    @GetMapping("updateDatabase")
    public List<Movie> updateDatabase() throws IOException, InterruptedException {
        return databaseService.updateDatabase();
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

    @GetMapping("/updateIndex")
    public String updateIndex() throws InterruptedException {
        return databaseService.updateIndex();
    }

    @GetMapping("/cleanDatabase")
    public List<Movie> cleanDatabase() {
        return databaseService.cleanDatabase();
    }


    @GetMapping("/searchPhrase")
    public Page<Line> searchLineIndexed2(
            @RequestParam("phrase") String phrase, Pageable pageable) {
        return searchService.search(phrase, pageable);
    }

    @GetMapping("/lineSnapshot")
    public Map<String, String> getLineSnapshot(
            @RequestParam("lineId") Long lineId,
            HttpServletRequest request
    ) throws LineNotFoundException, InterruptedException, MovieNotFoundException, IOException {
        return lineService.getSnapshot(lineId, request);
    }
}
