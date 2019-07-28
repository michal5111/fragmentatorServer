package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
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

import static com.michal5111.fragmentatorServer.utils.Utils.calculateDuration;
import static com.michal5111.fragmentatorServer.utils.Utils.getMovieExtension;

@Service
public class ConverterService {

    private Logger logger = LoggerFactory.getLogger(ConverterService.class);

    private String nameGenerator(Movie movie) {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        return String.valueOf((movie.getFileName()+line.getTextLines()+line.getNumber()+movie.getStartOffset()+movie.getStopOffset()).hashCode());
    }

    public String getSnapshot(Movie movie) throws IOException, InterruptedException, MovieNotFoundException {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        File file = new File("/home/michal/Obrazy/SpringFragmenterCache/"+nameGenerator(movie)+".jpg");
        if (file.exists()) {
            return nameGenerator(movie)+".jpg";
        }
        line.parseTime();
        movie.setExtension(getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
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

    public String generateFragmentLink(Movie movie) throws IOException, MovieNotFoundException {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        line.parseTime();
        movie.setExtension(getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        LocalTime time = line.getTimeFrom().minusMinutes(1).plusNanos((Double.valueOf(movie.getStartOffset()*1000000000.0)).longValue());
        double to =  calculateDuration(line.getTimeFrom(),line.getTimeTo(),movie.getStartOffset(),movie.getStopOffset());
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

    public Flux<ServerSentEvent<String>> convertFragment(Movie movie) throws IOException, MovieNotFoundException {

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

        Line line = movie.getSubtitles().getFilteredLines().get(0);
        File tempSubtitles = Utils.createTempSubtitles(movie);
        line.parseTime();
        movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        LocalTime time = line.getTimeFrom().minusMinutes(1)
                .plusNanos((Double.valueOf(movie.getStartOffset()*1000000000.0)).longValue());
        double to = Utils.calculateDuration(line.getTimeFrom(),line.getTimeTo(),movie.getStartOffset(),movie.getStopOffset());
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
            emmiter.next(ServerSentEvent.<String>builder().event("to").id("0").data(String.valueOf(to)).build());
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

    public Flux<ServerSentEvent<String>> convertDialog(Movie movie) throws IOException, MovieNotFoundException {

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

        Line firstLine = movie.getSubtitles().getFilteredLines().get(0);
        Line lastLine = movie.getSubtitles().getFilteredLines().get(movie.getSubtitles().getFilteredLines().size()-1);
        //File tempSubtitles = Utils.createTempSubtitles(movie);
        firstLine.parseTime();
        lastLine.parseTime();
        movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        LocalTime time = firstLine.getTimeFrom().minusMinutes(1)
                .plusNanos((Double.valueOf(movie.getStartOffset()*1000000000.0)).longValue());
        double to = Utils.calculateDuration(firstLine.getTimeFrom(),lastLine.getTimeTo(),movie.getStartOffset(),movie.getStopOffset());
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
                //"-vf", "subtitles="+tempSubtitles.getPath(),
                "/home/michal/Wideo/SpringFragmenterCache/"+fragmentName
        );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Flux<ServerSentEvent<String>> toEvent = Flux.create(emmiter -> {
            emmiter.next(ServerSentEvent.<String>builder().event("to").id("0").data(String.valueOf(to)).build());
            emmiter.complete();
        });
        Flux<ServerSentEvent<String>> percent = Flux.fromStream(reader.lines())
                .doOnNext(s -> logger.debug(s))
                .map(s -> ServerSentEvent.builder(s).event("log").data(s).id("1").build());
        Flux<ServerSentEvent<String>> complete = Flux.create(emmiter -> {
            emmiter.next(ServerSentEvent.<String>builder().event("complete").id("2").data(fragmentName).build());
            emmiter.complete();
            //tempSubtitles.delete();
        });
        return toEvent.concatWith(percent.concatWith(complete));
    }
}
