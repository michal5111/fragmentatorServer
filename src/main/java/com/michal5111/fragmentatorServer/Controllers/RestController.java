package com.michal5111.fragmentatorServer.Controllers;

import com.michal5111.fragmentatorServer.Entities.*;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.repositories.FragmentRequestRepository;
import com.michal5111.fragmentatorServer.repositories.LineRepository;
import com.michal5111.fragmentatorServer.repositories.MovieRepository;
import com.michal5111.fragmentatorServer.repositories.SubtitlesRepository;
import com.michal5111.fragmentatorServer.services.ConverterService;
import com.michal5111.fragmentatorServer.services.DatabaseService;
import com.michal5111.fragmentatorServer.utils.Utils;
import lombok.val;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("api")
public class RestController {

    private final Logger logger = LoggerFactory.getLogger(RestController.class);

    private final ConverterService converterService;

    private final MovieRepository movieRepository;

    private final SubtitlesRepository subtitlesRepository;

    private final LineRepository lineRepository;

    private final DatabaseService databaseService;

    private final FragmentRequestRepository fragmentRequestRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public RestController(ConverterService converterService, MovieRepository movieRepository, SubtitlesRepository subtitlesRepository, LineRepository lineRepository, DatabaseService databaseService, FragmentRequestRepository fragmentRequestRepository) {
        this.converterService = converterService;
        this.movieRepository = movieRepository;
        this.subtitlesRepository = subtitlesRepository;
        this.lineRepository = lineRepository;
        this.databaseService = databaseService;
        this.fragmentRequestRepository = fragmentRequestRepository;
    }

    @GetMapping("/test")
    public String test() throws IOException {
        Utils.test();
        return "";
    }

    @PostMapping("/fragmentRequest")
    public FragmentRequest createFragmentRequest(@RequestBody FragmentRequest fragmentRequest) {
        return fragmentRequestRepository.save(fragmentRequest);
    }

    @GetMapping(path = "/fragmentRequest/{id}")
    public Flux<ServerSentEvent<String>> requestFragment(@PathVariable("id") Long fragmentRequestId,
                                                         HttpServletRequest request) throws IOException, MovieNotFoundException {
        Optional<FragmentRequest> optionalFragmentRequest = fragmentRequestRepository.findById(fragmentRequestId);
        if (optionalFragmentRequest.isEmpty()) {
            throw new IllegalArgumentException();
        }
        FragmentRequest fragmentRequest = optionalFragmentRequest.get();
        List<Line> lines = lineRepository.findAllByIdBetween(fragmentRequest.getStartLine().getId(), fragmentRequest.getStopLine().getId());
        logger.info("Request for: " + fragmentRequest.getMovie() + " "
                + fragmentRequest.getStartLine().getTextLines());
        return converterService.convertFragment(fragmentRequest, lines);
    }

    @GetMapping(path = "/requestFragment")
    public Flux<ServerSentEvent<String>> requestFragment(@RequestParam String fileName,
                                                         @RequestParam String line,
                                                         @RequestParam String timeString,
                                                         @RequestParam String path,
                                                         @RequestParam Integer lineNumber,
                                                         @RequestParam Double startOffset,
                                                         @RequestParam Double stopOffset,
                                                         @RequestParam String subtitlesFileName,
                                                         HttpServletRequest request) throws IOException, MovieNotFoundException {
        logger.info("Request for: " + fileName + " " + line);
        logger.debug("Params: fileName: " + fileName
                + "\nline: " + line
                + "\ntimeString: " + timeString
                + "\npath: " + path
                + "\nlineNumber: " + lineNumber
                + "\nstartOffset: " + startOffset
                + "\nstopOffset: " + stopOffset
                + "\nsubtitlesFileName: " + subtitlesFileName);
        Line line1 = Line.builder()
                .number(lineNumber)
                .timeString(timeString)
                .textLines(line)
                .build();
        Subtitles subtitles = new SRTSubtitles();
        subtitles.setFilename(subtitlesFileName);
        subtitles.setFilteredLines(List.of(line1));
        Movie movie = Movie.builder()
                .fileName(fileName)
                .path(path)
                .subtitles(subtitles)
                .startOffset(startOffset)
                .stopOffset(stopOffset)
                .build();
        return converterService.convertFragment(movie);
    }

    @GetMapping(path = "/dialog")
    public Flux<ServerSentEvent<String>> requestDialog(@RequestParam String fileName,
                                                       @RequestParam List<String> line,
                                                       @RequestParam List<String> timeString,
                                                       @RequestParam String path,
                                                       @RequestParam List<Integer> lineNumber,
                                                       @RequestParam Double startOffset,
                                                       @RequestParam Double stopOffset,
                                                       @RequestParam String subtitlesFileName,
                                                       HttpServletRequest request) throws IOException, MovieNotFoundException {
        List<Line> linesList = new LinkedList<>();
        for (int i = 0; i < line.size(); i++) {
            Line tempLine = new Line();
            tempLine.setNumber(lineNumber.get(i));
            tempLine.setTextLines(line.get(i));
            tempLine.setTimeString(timeString.get(i));
            linesList.add(tempLine);
        }
        Subtitles subtitles = new SRTSubtitles();
        subtitles.setFilename(path+"/"+subtitlesFileName);
        subtitles.setFilteredLines(linesList);
        Movie movie = Movie.builder()
                .fileName(fileName)
                .path(path)
                .subtitles(subtitles)
                .startOffset(startOffset)
                .stopOffset(stopOffset)
                .build();
        return converterService.convertDialog(movie);
    }

    @GetMapping("updateDatabase")
    public List<Movie> updateDatabase() throws IOException, InterruptedException {
        return databaseService.updateDatabase();
    }

    @GetMapping("/lineHints")
    public List<Line> getLinesSQL(@RequestParam("phrase") String phrase) {
        return lineRepository.findText2(phrase);
    }

    @GetMapping("/movieHints")
    public List<Movie> getTitleHints(@RequestParam("title") String title) {
        return movieRepository.findTitleHints(title);
    }

//    @GetMapping("/searchPhrase")
//    public List<Movie> getMovieSQL(@RequestParam("phrase") String phrase) {
//        return movieRepository.findMoviesByPhrase(phrase);
//    }

    @GetMapping("/searchMovie")
    public List<Movie> getMovieByTitleSQL(@RequestParam("title") String title) {
        return movieRepository.findMovieByFileNameContainingIgnoreCase(title);
    }

    @GetMapping("/getLines")
    public List<Line> getLines(@RequestParam("movieId") Long movieId) {
        return lineRepository.findAllBySubtitles_Movie_Id(movieId);
    }

    @GetMapping("/getFilteredLines")
    public List<Line> getFilteredLines(@RequestParam("subtitlesId") Long movieId,@RequestParam("phrase") String phrase) {
        return lineRepository.findFilteredLines(movieId,phrase);
    }

    @GetMapping("/updateIndex")
    public String updateIndex() throws InterruptedException {
        return databaseService.updateIndex();
    }

    @GetMapping("/searchPhrase")
    public Set<Movie> searchLineIndexed(@RequestParam("phrase") String phrase) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Line.class)
                .get();
        Query query = queryBuilder
                .phrase()
                .withSlop(2)
                //.simpleQueryString()
                .onField("textLines")
                .sentence(phrase)
                //.matching(phrase)
                .createQuery();
        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query,Line.class);
        //jpaQuery.setMaxResults(100);
        jpaQuery.setSort(Sort.RELEVANCE);
        List<Line> resultList = jpaQuery.getResultList();
        Set<Movie> movieSet = new HashSet<>();
        resultList.forEach(line -> {
            Subtitles subtitles = line.getSubtitles();
            subtitles.getFilteredLines().add(line);
            Movie movie = subtitles.getMovie();
            movieSet.add(movie);
        });
        return movieSet;
    }

    @GetMapping("/searchPhrase2")
    public List searchLineIndexed2(@RequestParam("phrase") String phrase, @RequestParam("firstResult") int firstResult, @RequestParam("maxResults") int maxResults) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Line.class)
                .get();
        Query query = queryBuilder
                .phrase()
                .withSlop(2)
                //.simpleQueryString()
                .onField("textLines")
                .sentence(phrase)
                //.matching(phrase)
                .createQuery();
        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query,Line.class);
        jpaQuery.setFirstResult(firstResult);
        jpaQuery.setMaxResults(maxResults);
        jpaQuery.setSort(Sort.RELEVANCE);
        return jpaQuery.getResultList();
    }

    @GetMapping("/lineSnapshot")
    public String getLineSnapshot(@RequestParam("lineId") Long lineId, HttpServletRequest request, HttpServletResponse response) throws IOException, InterruptedException, MovieNotFoundException {
        Optional<Line> optionalLine = lineRepository.findById(lineId);
        if (optionalLine.isEmpty()) {
            throw new IllegalArgumentException();
        }
        Line line = optionalLine.get();
        return "{\"url\":\"http://" + request.getServerName() + ":" + request.getServerPort() + "/fragmentatorServer/snapshots/" + converterService.getSnapshot(line) + "\"}";
    }
}
