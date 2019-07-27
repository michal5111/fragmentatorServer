package com.michal5111.fragmentatorServer.Controllers;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.Entities.SRTSubtitles;
import com.michal5111.fragmentatorServer.Entities.Subtitles;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.services.ConverterService;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("api")
public class RestController {

    private Logger logger = LoggerFactory.getLogger(RestController.class);

    @Autowired
    private
    ConverterService converterService;

    @GetMapping("/search")
    public List<Movie> getMoviesList(@RequestParam("fraze") String fraze) {
        Long startTime = System.nanoTime();
        List<Movie> movieList = new LinkedList<>();
        try {
            movieList = Utils.findFraze(fraze);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Long stopTime = System.nanoTime();
        System.out.println((stopTime - startTime) / 1000000 + "ms");
        return movieList;
    }

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
                .startOffset(startOffset)
                .stopOffset(stopOffset)
                .build();
        Subtitles subtitles = new SRTSubtitles();
        subtitles.setFilename(subtitlesFileName);
        subtitles.setFilteredLines(List.of(line1));
        Movie movie = Movie.builder()
                .fileName(fileName)
                .path(path)
                .subtitles(subtitles)
                .build();
        return converterService.convertFragment(movie);
    }

    @GetMapping(path = "/dialog")
    public Flux<ServerSentEvent<String>> requestDialog(@RequestParam String fileName,
                                                       @RequestParam List<String> lines,
                                                       @RequestParam List<String> timeStrings,
                                                       @RequestParam String path,
                                                       @RequestParam List<Integer> lineNumbers,
                                                       @RequestParam Double startOffset,
                                                       @RequestParam Double stopOffset,
                                                       @RequestParam String subtitlesFileName,
                                                       HttpServletRequest request) throws IOException, MovieNotFoundException {
        List<Line> linesList = new LinkedList<>();
        Line firstLine = new Line();
        firstLine.setStartOffset(startOffset);
        firstLine.setNumber(lineNumbers.get(0));
        firstLine.setTextLines(lines.get(0));
        firstLine.setTimeString(timeStrings.get(0));
        linesList.add(firstLine);
        for (int i = 1; i < lines.size() - 1; i++) {
            Line line = new Line();
            line.setNumber(lineNumbers.get(i));
            line.setTextLines(lines.get(i));
            line.setTimeString(timeStrings.get(i));
            linesList.add(line);
        }
        Line lastLine = new Line();
        lastLine.setNumber(lineNumbers.get(lineNumbers.size() - 1));
        lastLine.setTextLines(lines.get(lines.size() - 1));
        lastLine.setTimeString(timeStrings.get(timeStrings.size() - 1));
        lastLine.setStopOffset(stopOffset);
        linesList.add(lastLine);
        Subtitles subtitles = new SRTSubtitles();
        subtitles.setFilename(subtitlesFileName);
        subtitles.setFilteredLines(linesList);
        Movie movie = Movie.builder()
                .fileName(fileName)
                .path(path)
                .subtitles(subtitles)
                .build();
        return converterService.convertDialog(movie);
    }

    @GetMapping("/searchMovie")
    public List<Movie> searchByMovie(@RequestParam("fraze") String fraze) {
        Long startTime = System.nanoTime();
        List<Movie> movieList = new LinkedList<>();
        try {
            movieList = Utils.findMovie(fraze);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Long stopTime = System.nanoTime();
        System.out.println((stopTime - startTime) / 1000000 + "ms");
        return movieList;
    }
}
