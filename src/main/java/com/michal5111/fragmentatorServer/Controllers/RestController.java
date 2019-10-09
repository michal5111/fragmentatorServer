package com.michal5111.fragmentatorServer.Controllers;

import com.michal5111.fragmentatorServer.domain.FragmentRequest;
import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Movie;
import com.michal5111.fragmentatorServer.exceptions.FragmentRequestNotFoundException;
import com.michal5111.fragmentatorServer.exceptions.LineNotFoundException;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.repositories.FragmentRequestRepository;
import com.michal5111.fragmentatorServer.repositories.LineRepository;
import com.michal5111.fragmentatorServer.repositories.MovieRepository;
import com.michal5111.fragmentatorServer.repositories.SubtitlesRepository;
import com.michal5111.fragmentatorServer.services.ConverterService;
import com.michal5111.fragmentatorServer.services.DatabaseService;
import com.michal5111.fragmentatorServer.utils.Utils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public Flux<ServerSentEvent<String>> requestFragment(
            @PathVariable("id") Long fragmentRequestId,
            HttpServletRequest request
    ) throws FragmentRequestNotFoundException, InterruptedException, MovieNotFoundException, IOException {
        Optional<FragmentRequest> optionalFragmentRequest = fragmentRequestRepository.findById(fragmentRequestId);
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

//    @GetMapping("/searchPhrase")
//    public Set<SearchPhraseResponse> searchLineIndexed(@RequestParam("phrase") String phrase) {
//        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
//        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
//                .buildQueryBuilder()
//                .forEntity(Line.class)
//                .get();
//        Query query = queryBuilder
//                .phrase()
//                .withSlop(2)
//                //.simpleQueryString()
//                .onField("textLines")
//                .sentence(phrase)
//                //.matching(phrase)
//                .createQuery();
//        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query,Line.class);
//        //jpaQuery.setMaxResults(100);
//        jpaQuery.setSort(Sort.RELEVANCE);
//        List<Line> resultList = jpaQuery.getResultList();
//        Set<SearchPhraseResponse> searchPhraseResponses = new HashSet<>();
//        resultList.forEach(line -> {
//            SearchPhraseResponse searchPhraseResponse = new SearchPhraseResponse();
//            Subtitles subtitles = line.getSubtitles();
//            Movie movie = subtitles.getMovie();
//            searchPhraseResponse.setMovie(movie);
//            searchPhraseResponses.add(movie);
//        });
//        return searchPhraseResponses;
//    }

    @GetMapping("/searchPhrase")
    public List searchLineIndexed2(
            @RequestParam("phrase") String phrase,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults) {
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
    public Map<String, String> getLineSnapshot(
            @RequestParam("lineId") Long lineId,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws LineNotFoundException, InterruptedException, MovieNotFoundException, IOException {
        Optional<Line> optionalLine = lineRepository.findById(lineId);
        if (optionalLine.isEmpty()) {
            throw new LineNotFoundException("Line Not Found!");
        }
        Line line = optionalLine.get();
        Map<String, String> map = new HashMap<>();
        map.put("url","http://" + request.getServerName() + ":" + request.getServerPort() + "/fragmentatorServer/snapshots/" + converterService.getSnapshot(line));
        return map;
    }
}
