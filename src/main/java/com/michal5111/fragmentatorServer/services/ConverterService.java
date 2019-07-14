package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.michal5111.fragmentatorServer.utils.Utils.getMovieExtension;

@Service
public class ConverterService {

    private Logger logger = LoggerFactory.getLogger(ConverterService.class);

    private String nameGenerator(Movie movie) {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        return String.valueOf((movie.getFileName()+line.getNumber()+line.getStartOffset()+line.getStopOffset()).hashCode());
    }

    public String getSnapshot(Movie movie) throws IOException, InterruptedException {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        File file = new File("/home/michal/Obrazy/SpringFragmenterCache/"+nameGenerator(movie)+".jpg");
        if (file.exists()) {
            return nameGenerator(movie)+".jpg";
        }
        line.parseTime();
        movie.setExtension(getMovieExtension(Paths.get(movie.getPath())));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        String timeString = dateTimeFormatter.format(line.getTimeFrom());
        File tempSubtitles = Utils.createTempSubtitles(movie);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-n",
                "-ss", timeString,
                "-i", movie.getPath()+File.separator+movie.getFileName()+movie.getExtension(),
                "-vf","subtitles="+tempSubtitles.getPath(),
                "-frames:v", "1",
                "/home/michal/Obrazy/SpringFragmenterCache/"+nameGenerator(movie)+".jpg"
        );
        Process process = processBuilder.start();
        process.waitFor();
        logger.debug(String.valueOf(process.exitValue()));
        tempSubtitles.delete();
        return nameGenerator(movie)+".jpg";
    }

    public String generateFragmentLink(Movie movie) throws IOException {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        line.parseTime();
        movie.setExtension(getMovieExtension(Paths.get(movie.getPath())));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        LocalTime time = line.getTimeFrom().minusMinutes(1).plusNanos((Double.valueOf(line.getStartOffset()*1000000000.0)).longValue());
        double to =  line.getTimeTo().getHour()*3600+line.getTimeTo().getMinute()*60
                +line.getTimeTo().getSecond()
                +line.getTimeTo().getNano()/1000000000.0
                +line.getStopOffset()
                - (line.getTimeFrom().getHour()*3600
                +line.getTimeFrom().getMinute()*60
                +line.getTimeFrom().getSecond()
                +line.getTimeFrom().getNano()/1000000000.0
                +line.getStartOffset());
        String timeString = dateTimeFormatter.format(time);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-y",
                "-ss", timeString,
                "-i", movie.getPath()+"/"+movie.getFileName()+movie.getExtension(),
                "-ss", "00:01:00",
                "-t", String.format(Locale.US, "%.3f", to),
                "-acodec", "copy",
                "-vcodec", "h264",
                "-preset", "veryslow",
                "/home/michal/Wideo/SpringFragmenterCache/"+nameGenerator(movie)+".mp4"
        );
        Process process = processBuilder.start();
        return movie.getFileName()+line.getNumber();
    }

    public Flux<ServerSentEvent<String>> convertFragment(Movie movie) throws IOException {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        File tempSubtitles = Utils.createTempSubtitles(movie);

        String fragmentName = nameGenerator(movie) + ".mp4";

        Path fragmentPath = Path.of("/home/michal/Wideo/SpringFragmenterCache/"+fragmentName);
        if (fragmentPath.toFile().exists()) {
            logger.debug("File exist");
            return Flux.create(emmiter -> {
                emmiter.next(ServerSentEvent.<String>builder().event("complete").id("2").data(fragmentName).build());
                emmiter.complete();
            });
        } else {
            logger.debug("File not exist"+"/home/michal/Wideo/SpringFragmenterCache/"+File.separator+fragmentName);
        }

        line.parseTime();
        movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath())));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        LocalTime time = line.getTimeFrom().minusMinutes(1)
                .plusNanos((Double.valueOf(line.getStartOffset()*1000000000.0)).longValue());
        double to = line.getTimeTo().getHour()*3600
                + line.getTimeTo().getMinute()*60
                + line.getTimeTo().getSecond()
                + line.getTimeTo().getNano()/1000000000.0
                + line.getStopOffset()
                - (line.getTimeFrom().getHour()*3600
                + line.getTimeFrom().getMinute()*60
                + line.getTimeFrom().getSecond()
                + line.getTimeFrom().getNano()/1000000000.0
                + line.getStartOffset());
        String timeString2 = dateTimeFormatter.format(time);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-y",
                "-ss", timeString2,
                "-i", movie.getPath()+"/"+movie.getFileName()+movie.getExtension(),
                "-ss", "00:01:00",
                "-t", String.format(Locale.US, "%.3f", to),
                "-acodec", "aac",
                "-vcodec", "h264",
                "-preset", "veryslow",
                "-vf", "subtitles="+tempSubtitles.getPath(),
                "/home/michal/Wideo/SpringFragmenterCache/"+fragmentName
        );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Flux<ServerSentEvent<String>> toEvent = Flux.create(emmiter -> {
            emmiter.next(ServerSentEvent.<String>builder().event("to").id("2").data(String.valueOf(to)).build());
            emmiter.complete();
        });
        Flux<ServerSentEvent<String>> percent = Flux.fromStream(reader.lines())
                .doOnNext(s -> logger.debug(s))
                .map(s -> ServerSentEvent.builder(s).event("log").data(s).id("1").build());
        Flux<ServerSentEvent<String>> complete = Flux.create(emmiter -> {
            emmiter.next(ServerSentEvent.<String>builder().event("complete").id("2").data(fragmentName).build());
            emmiter.complete();
            tempSubtitles.delete();
        });
        return toEvent.concatWith(percent.concatWith(complete));
    }
}
