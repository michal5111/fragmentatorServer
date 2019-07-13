package com.michal5111.fragmentatorServer.Controllers;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.Entities.SRTSubtitles;
import com.michal5111.fragmentatorServer.Entities.Subtitles;
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
@RequestMapping("rest")
public class RestController {

    Logger logger = LoggerFactory.getLogger(RestController.class);

    @Autowired
    ConverterService converterService;

    @GetMapping("/search")
    public List<Movie> getMoviesList(@RequestParam("fraze") String fraze) {
        List<Movie> movieList = new LinkedList<>();
        try {
            movieList = Utils.findFraze(fraze);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return movieList;
    }

    @PostMapping("/linesnapshot")
    public String getLineSnapshot(@RequestBody() Movie movie, HttpServletRequest request) throws IOException, InterruptedException {
        return "{\"url\":\"http://" + request.getServerName() +":"+ request.getServerPort() + "/snapshots/"
                + converterService.generateSnapshotLink(movie)+"\"}";
    }

    @PostMapping("/fragment")
    public String getfragment(@RequestBody() Movie movie, HttpServletRequest request) throws IOException {
        return "{\"url\":\"http://" + request.getServerName() +":"+ request.getServerPort() + "/fragments/"
                + converterService.generateFragmentLink(movie)+"\"}";
    }

    @GetMapping(path = "/requestFragment")
    public Flux<ServerSentEvent<String>> requestFragment(@RequestParam String fileName,
                                                         @RequestParam String line,
                                                         @RequestParam String timeString,
                                                         @RequestParam String path,
                                                         @RequestParam Integer lineNumber,
                                                         @RequestParam Double startOffset,
                                                         @RequestParam Double stopOffset,
                                                         @RequestParam String subtitlesFileName) throws IOException {
        logger.info("Request for: "+ fileName + " " + line);
        Line line1 = Line.builder()
                .number(lineNumber)
                .timeString(timeString)
                .textLines(line)
                .startOffset(startOffset)
                .startOffset(stopOffset)
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
}
