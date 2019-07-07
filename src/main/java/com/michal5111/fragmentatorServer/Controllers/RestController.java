package com.michal5111.fragmentatorServer.Controllers;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.Entities.SRTSubtitlesFile;
import com.michal5111.fragmentatorServer.Entities.SubtitlesFile;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("rest")
public class RestController {

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
        return "{\"url\":\"http://" + request.getServerName() +":"+ request.getServerPort() + "/snapshots/" + Utils.generateSnapshotLink(movie)+"\"}";
    }

    @PostMapping("/fragment")
    public String getfragment(@RequestBody() Movie movie, HttpServletRequest request) throws IOException {
        return "{\"url\":\"http://" + request.getServerName() +":"+ request.getServerPort() + "/fragments/" + Utils.generateFragmentLink(movie)+"\"}";
    }

    @GetMapping(path = "/requestFragment", params = {"fileName","line","timeString","path"})
    public Flux<ServerSentEvent<String>> requestFragment(@RequestParam String fileName, @RequestParam String line, @RequestParam String timeString, @RequestParam String path) {
        Movie movie = new Movie();
        movie.setPath(path);
        movie.setFileName(fileName);
        SubtitlesFile subtitlesFile = new SRTSubtitlesFile();
        Line lineObject = new Line();
        lineObject.setTimeString(timeString);
        lineObject.setTextLines(line);
        List<Line> lineList = new LinkedList<>();
        lineList.add(lineObject);
        subtitlesFile.setFilteredLines(lineList);
        movie.setSubtitles(subtitlesFile);

        lineObject.parseTime();
        try {
            movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        LocalTime time = lineObject.getTimeFrom().minusMinutes(1).plusNanos((new Double(lineObject.getStartOffset()*1000000000.0)).longValue());
        double to =  lineObject.getTimeTo().getHour()*3600+lineObject.getTimeTo().getMinute()*60+lineObject.getTimeTo().getSecond()+lineObject.getTimeTo().getNano()/1000000000.0+lineObject.getStopOffset()
                - (lineObject.getTimeFrom().getHour()*3600+lineObject.getTimeFrom().getMinute()*60+lineObject.getTimeFrom().getSecond()+lineObject.getTimeFrom().getNano()/1000000000.0+lineObject.getStartOffset());
        String timeString2 = dateTimeFormatter.format(time);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                //"konsole",
                //"--hold",
                //"-e",
                "ffmpeg",
                "-y",
                "-ss", timeString2,
                "-i", movie.getPath()+"/"+movie.getFileName()+movie.getExtension(),
                "-ss", "00:01:00",
                "-t", String.format(Locale.US, "%.3f", to),
                "-acodec", "copy",
                "-vcodec", "h264",
                "-preset", "veryslow",
                //"-vf", "subtitles=TEMP.srt",
                "/home/michal/Wideo/SpringFragmenterCache/"+(movie.getFileName()+lineObject.getNumber()).hashCode()+".mp4"
        );
        processBuilder.redirectErrorStream(true);

        return Flux.create(emmiter -> {
            emmiter.next(ServerSentEvent.<String>builder().event("test").id("1").data("robi się").build());
            try {
                Process process = processBuilder.start();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                reader.lines().forEach(lineString -> emmiter.next(ServerSentEvent.<String>builder().event("lala").id("1").data(lineString).build()));
                emmiter.next(ServerSentEvent.<String>builder().event("complete").id("2").data((movie.getFileName()+lineObject.getNumber()).hashCode()+".mp4").build());
                //emmiter.complete();
            } catch (IOException e) {
                e.printStackTrace();
                emmiter.error(e);
            }
        });
    }
}
