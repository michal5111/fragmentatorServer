package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.utils.Properties;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import java.util.stream.Stream;

import static com.michal5111.fragmentatorServer.utils.Utils.getMovieExtension;

@EnableConfigurationProperties(Properties.class)
@Service
public class ConverterService {

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Logger logger = LoggerFactory.getLogger(ConverterService.class);

    private String nameGenerator(Movie movie) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(movie.getFileName());
        stringBuilder.append(movie.getStartOffset());
        stringBuilder.append(movie.getStopOffset());
        movie.getSubtitles().getFilteredLines().forEach(line -> {
            stringBuilder.append(line.getTextLines());
            stringBuilder.append(line.getNumber());
            stringBuilder.append(line.getTimeFrom());
            stringBuilder.append(line.getTimeTo());
        });
        return String.valueOf(stringBuilder.toString().hashCode());
    }

    private ProcessBuilder prepareProcess(LocalTime timeFrom,
                                          LocalTime timeTo,
                                          String input,
                                          File subtitles,
                                          String output,
                                          Double startOffset,
                                          Double stopOffset) {
        LocalTime timeF = timeFrom.minusSeconds(1)
                .plusNanos((Double.valueOf(startOffset*1000000000.0)).longValue());
        double timeT = Utils.calculateDuration(timeFrom,timeTo,startOffset,stopOffset);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-hide_banner",
                "-y",
                "-ss", dateTimeFormatter.format(timeF),
                "-i", input,
                "-ss", "00:00:01",
                "-t", String.format(Locale.US, "%.3f", timeT),
                "-acodec", "aac",
                "-vcodec", "h264",
                "-preset", "veryslow",
                "-vf", "subtitles="+subtitles.getPath(),
                output
        );
        processBuilder.redirectErrorStream(true);
        return processBuilder;
    }

    public String getSnapshot(Movie movie) throws IOException, InterruptedException, MovieNotFoundException {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        File file = new File("/home/michal/Obrazy/SpringFragmenterCache/"+nameGenerator(movie)+".jpg");
        if (file.exists()) {
            return nameGenerator(movie)+".jpg";
        }
        line.parseTime();
        movie.setExtension(getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
        String timeString = dateTimeFormatter.format(line.getTimeFrom());
        File tempSubtitles = Utils.createTempSubtitles(movie);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-hide_banner",
                "-n",
                "-ss", timeString,
                "-i", movie.getPath()+File.separator+movie.getFileName()+movie.getExtension(),
                "-vf","subtitles="+tempSubtitles.getPath(),
                "-frames:v", "1",
                "/home/michal/Obrazy/SpringFragmenterCache/"+nameGenerator(movie)+".jpg"
        );
        Process process = processBuilder.start();
        process.waitFor();
        logger.debug("Exit value: "+ process.exitValue());
        tempSubtitles.delete();
        return nameGenerator(movie)+".jpg";
    }

//    public String generateFragmentLink(Movie movie) throws IOException, MovieNotFoundException {
//        Line line = movie.getSubtitles().getFilteredLines().get(0);
//        line.parseTime();
//        movie.setExtension(getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
//        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
//        LocalTime time = line.getTimeFrom().minusMinutes(1).plusNanos((Double.valueOf(movie.getStartOffset()*1000000000.0)).longValue());
//        double to =  calculateDuration(line.getTimeFrom(),line.getTimeTo(),movie.getStartOffset(),movie.getStopOffset());
//        String timeString = dateTimeFormatter.format(time);
//        ProcessBuilder processBuilder = new ProcessBuilder();
//        processBuilder.command(
//                "ffmpeg",
//                "-y",
//                "-ss", timeString,
//                "-i", movie.getPath()+"/"+movie.getFileName()+movie.getExtension(),
//                "-ss", "00:01:00",
//                "-t", String.format(Locale.US, "%.3f", to),
//                "-acodec", "copy",
//                "-vcodec", "h264",
//                "-preset", "veryslow",
//                "/home/michal/Wideo/SpringFragmenterCache/"+nameGenerator(movie)+".mp4"
//        );
//        Process process = processBuilder.start();
//        return movie.getFileName()+line.getNumber();
//    }

    public Flux<ServerSentEvent<String>> convertFragment(Movie movie) throws IOException, MovieNotFoundException {

        String fragmentName = nameGenerator(movie) + ".mp4";

        Path fragmentPath = Path.of("/home/michal/Wideo/SpringFragmenterCache/"+fragmentName);
        if (fragmentPath.toFile().exists()) {
            logger.debug("File exist");
            return Flux.create(emitter -> {
                emitter.next(ServerSentEvent.<String>builder().event("complete").id("2").data(fragmentName).build());
                emitter.complete();
            });
        } else {
            logger.debug("File not exist"+"/home/michal/Wideo/SpringFragmenterCache/"+File.separator+fragmentName);
        }

        Line line = movie.getSubtitles().getFilteredLines().get(0);
        File tempSubtitles = Utils.createTempSubtitles(movie);
        line.parseTime();
        movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
        LocalTime time = line.getTimeFrom().minusSeconds(1)
                .plusNanos((Double.valueOf(movie.getStartOffset()*1000000000.0)).longValue());
        double to = Utils.calculateDuration(line.getTimeFrom(),line.getTimeTo(),movie.getStartOffset(),movie.getStopOffset());
        String timeString2 = dateTimeFormatter.format(time);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-hide_banner",
                "-y",
                "-ss", timeString2,
                "-i", movie.getPath()+"/"+movie.getFileName()+movie.getExtension(),
                "-ss", "00:00:01",
                "-t", String.format(Locale.US, "%.3f", to),
                "-acodec", "aac",
                "-vcodec", "h264",
                "-preset", "veryslow",
                "-vf", "subtitles="+tempSubtitles.getPath(),
                "/home/michal/Wideo/SpringFragmenterCache/"+fragmentName
        );
        processBuilder.redirectErrorStream(true);

//        ProcessBuilder processBuilder2 = prepareProcess(line.getTimeFrom(),
//                movie.getPath()+"/"+movie.getFileName()+movie.getExtension(),
//                )

        Process process = processBuilder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Stream<String> stringStream = reader.lines().peek(s -> {
            if (s.contains("Conversion failed!")) {
                File fragmentFile = new File("/home/michal/Wideo/SpringFragmenterCache/"+fragmentName);
                fragmentFile.delete();
                throw new IllegalStateException("Conversion failed!");
            }
        });
        Flux<ServerSentEvent<String>> toEvent = Flux.create(emitter -> {
            emitter.next(ServerSentEvent.<String>builder().event("to").id("0").data(String.valueOf(to)).build());
            emitter.complete();
        });
        Flux<ServerSentEvent<String>> percent = Flux.fromStream(stringStream)
                .doOnNext(s -> logger.debug(s))
                .map(s -> ServerSentEvent.builder(s).event("log").data(s).id("1").build());
        Flux<ServerSentEvent<String>> complete = Flux.create(emitter -> {
            emitter.next(ServerSentEvent.<String>builder().event("complete").id("2").data(fragmentName).build());
            emitter.complete();
            tempSubtitles.delete();
        });
        return toEvent.concatWith(percent.concatWith(complete)).doOnError(s -> ServerSentEvent.builder(s).event("error").data(s).id("2").build());
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
        firstLine.parseTime();
        lastLine.parseTime();
        movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
        LocalTime time = firstLine.getTimeFrom().minusSeconds(1)
                .plusNanos((Double.valueOf(movie.getStartOffset()*1000000000.0)).longValue());
        double to = Utils.calculateDuration(firstLine.getTimeFrom(),lastLine.getTimeTo(),movie.getStartOffset(),movie.getStopOffset());
        String timeString2 = dateTimeFormatter.format(time);
        ProcessBuilder subtitlesProcessBuilder = new ProcessBuilder();
        subtitlesProcessBuilder.command(
                "ffmpeg",
                "-y",
                "-i", movie.getSubtitles().getFilename(),
                "-ss", timeString2,
                "/tmp/temp.srt"
        );
        Process subtitleProcess = subtitlesProcessBuilder.start();
        try {
            subtitleProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-y",
                "-ss", timeString2,
                "-i", movie.getPath()+"/"+movie.getFileName()+movie.getExtension(),
                "-ss", "00:00:01",
                "-t", String.format(Locale.US, "%.3f", to),
                "-acodec", "aac",
                "-vcodec", "h264",
                "-preset", "veryslow",
                "-vf", "subtitles=/tmp/temp.srt",
                "/home/michal/Wideo/SpringFragmenterCache/"+fragmentName
        );
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Stream<String> stringStream = reader.lines().peek(s -> {
            if (s.contains("Conversion failed!")) {
                File fragmentFile = new File("/home/michal/Wideo/SpringFragmenterCache/"+fragmentName);
                fragmentFile.delete();
                throw new IllegalStateException("Conversion failed!");
            }
        });
        Flux<ServerSentEvent<String>> toEvent = Flux.create(emitter -> {
            emitter.next(ServerSentEvent.<String>builder().event("to").id("0").data(String.valueOf(to)).build());
            emitter.complete();
        });
        Flux<ServerSentEvent<String>> percent = Flux.fromStream(stringStream)
                .doOnNext(s -> logger.debug(s))
                .map(s -> ServerSentEvent.builder(s).event("log").data(s).id("1").build());
        Flux<ServerSentEvent<String>> complete = Flux.create(emmiter -> {
            emmiter.next(ServerSentEvent.<String>builder().event("complete").id("2").data(fragmentName).build());
            emmiter.complete();
        });
        return toEvent.concatWith(percent.concatWith(complete))
                .doOnError(s -> ServerSentEvent.builder(s).event("error").data(s).id("2").build());
    }
}
