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
    public Flux<ServerSentEvent<String>> requestFragment(@RequestParam String fileName, @RequestParam String line, @RequestParam String timeString, @RequestParam String path, @RequestParam int lineNumber) throws IOException {
        Movie movie = new Movie();
        movie.setPath(path);
        movie.setFileName(fileName);
        SubtitlesFile subtitlesFile = new SRTSubtitlesFile();
        Line lineObject = new Line();
        lineObject.setTimeString(timeString);
        lineObject.setTextLines(line);
        lineObject.setNumber(lineNumber);
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
        LocalTime time = lineObject.getTimeFrom().minusMinutes(1).plusNanos((Double.valueOf(lineObject.getStartOffset()*1000000000.0)).longValue());
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
                //"-vf", "subtitles="+movie.getPath()+"/"+movie.getFileName()+".srt",
                "/home/michal/Wideo/SpringFragmenterCache/"+(movie.getFileName()+lineObject.getNumber()).hashCode()+".mp4"
        );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Flux<ServerSentEvent<String>> toEvent = Flux.create(emmiter -> {
            emmiter.next(ServerSentEvent.<String>builder().event("to").id("2").data(String.valueOf(to)).build());
            emmiter.complete();
        });
        Flux<ServerSentEvent<String>> percent = Flux.fromStream(reader.lines())
//                .filterWhen(s -> Mono.just(s.contains("frame=")),1)
//                .map(s -> s.substring(s.lastIndexOf("time=")+5,s.lastIndexOf("time=")+16))
//                .map(s -> String.valueOf(Utils.timeToSeconds(s)*100/to))
                .doOnNext(System.out::println)
                .map(s -> ServerSentEvent.builder(s).event("log").data(s).id("1").build());
        Flux<ServerSentEvent<String>> complete = Flux.create(emmiter -> {
            emmiter.next(ServerSentEvent.<String>builder().event("complete").id("2").data((movie.getFileName() + lineObject.getNumber()).hashCode() + ".mp4").build());
            emmiter.complete();
        });
        return toEvent.concatWith(percent.concatWith(complete));
    }
}
