package com.michal5111.fragmentatorServer.Controllers;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.Entities.SRTSubtitles;
import com.michal5111.fragmentatorServer.Entities.Subtitles;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.repositories.LineRepository;
import com.michal5111.fragmentatorServer.repositories.MovieRepository;
import com.michal5111.fragmentatorServer.repositories.SubtitlesRepository;
import com.michal5111.fragmentatorServer.services.ConverterService;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("api")
public class RestController {

    private Logger logger = LoggerFactory.getLogger(RestController.class);

    private final ConverterService converterService;

    private final MovieRepository movieRepository;

    private final SubtitlesRepository subtitlesRepository;

    private final LineRepository lineRepository;

    public RestController(ConverterService converterService, MovieRepository movieRepository, SubtitlesRepository subtitlesRepository, LineRepository lineRepository) {
        this.converterService = converterService;
        this.movieRepository = movieRepository;
        this.subtitlesRepository = subtitlesRepository;
        this.lineRepository = lineRepository;
    }

//    @GetMapping("/searchFraze")
//    public List<Movie> getMoviesList(@RequestParam("fraze") String fraze) {
//        logger.info("Search for:" + fraze);
//        Long startTime = System.nanoTime();
//        List<Movie> movieList = new LinkedList<>();
//        try {
//            movieList = Utils.findFraze(fraze);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Long stopTime = System.nanoTime();
//        System.out.println((stopTime - startTime) / 1000000 + "ms");
//        return movieList;
//    }

    @GetMapping("/test")
    public String test() throws IOException {
        Utils.test();
        return "";
    }

    @PostMapping("/linesnapshot")
    public String getLineSnapshot(@RequestBody() Movie movie, HttpServletRequest request) throws IOException, InterruptedException, MovieNotFoundException {
        return "{\"url\":\"http://" + request.getServerName() + ":" + request.getServerPort() + "/snapshots/"
                + converterService.getSnapshot(movie) + "\"}";
    }

    @PostMapping("/fragment")
    public String getfragment(@RequestBody() Movie movie, HttpServletRequest request) throws IOException, MovieNotFoundException {
        return "{\"url\":\"http://" + request.getServerName() + ":" + request.getServerPort() + "/fragments/"
                + converterService.generateFragmentLink(movie) + "\"}";
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

//    @GetMapping("/searchMovie")
//    public List<Movie> searchByMovie(@RequestParam("title") String title) {
//        Long startTime = System.nanoTime();
//        List<Movie> movieList = new LinkedList<>();
//        try {
//            movieList = Utils.findMovie(title);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Long stopTime = System.nanoTime();
//        System.out.println((stopTime - startTime) / 1000000 + "ms");
//        return movieList;
//    }

//    @GetMapping("/subtitles")
//    public List<Line> getLines(@RequestParam("fileName") String fileName) {
//        List<Line> lineList = new LinkedList<>();
//        try {
//            lineList = Utils.getLines(fileName);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        return lineList;
//    }

    @GetMapping("/buildDatabase")
    public List<Movie> buildDatabase() throws IOException {
        List<Movie> movieList = Utils.findMovies().peek(movie -> {
            movie.getSubtitles().getLines().forEach(Line::parseTime);
        }).collect(Collectors.toList());
        return movieRepository.saveAll(movieList);
    }

    @GetMapping("updateDatabase")
    public List<Movie> updateDatabase() throws IOException {
        List<Movie> addedMovies = new LinkedList<>();
        Utils.findMovies().forEach(movie -> {
            Optional<Movie> optionalMovie = movieRepository
                    .findByPathAndFileNameEquals(movie.getPath(),movie.getFileName());
            if (optionalMovie.isPresent()) {
                return;
            }
            System.out.println("Adding movie: " + movie.getPath()+"/"+movie.getFileName());
            movie.getSubtitles().getLines().forEach(Line::parseTime);

            addedMovies.add(movie);
            movieRepository.save(movie);
        });
        return addedMovies;
    }

    @GetMapping("/lineHints")
    public List<Line> getLinesSQL(@RequestParam("fraze") String fraze) {
        return lineRepository.findText2(fraze);
    }

    @GetMapping("/movieHints")
    public List<Movie> getTitleHints(@RequestParam("title") String title) {
        return movieRepository.findTitleHints(title);
    }

    @GetMapping("/searchFraze")
    public List<Movie> getMovieSQL(@RequestParam("fraze") String fraze) {
        return movieRepository.findMoviesByFraze(fraze);
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
    public List<Line> getFilteredLines(@RequestParam("subtitlesId") Long movieId,@RequestParam("fraze") String fraze) {
        return lineRepository.findFilteredLines(movieId,fraze);
    }
}
