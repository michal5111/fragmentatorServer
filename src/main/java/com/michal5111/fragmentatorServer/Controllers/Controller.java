package com.michal5111.fragmentatorServer.Controllers;

import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("rest")
public class Controller {

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
    public String getLineSnapshot(@RequestBody() Movie movie, HttpServletRequest request) throws IOException {
        return "{\"url\":\"http://" + request.getServerName() +":"+ request.getServerPort() + "/snapshots/" + Utils.generateSnapshotLink(movie)+".jpg\"}";
    }

    @PostMapping("/fragment")
    public String getfragment(@RequestBody() Movie movie, HttpServletRequest request) throws IOException {
        return "{\"url\":\"http://" + request.getServerName() +":"+ request.getServerPort() + "/fragments/" + Utils.generateFragmentLink(movie)+".mp4\"}";
    }
}
