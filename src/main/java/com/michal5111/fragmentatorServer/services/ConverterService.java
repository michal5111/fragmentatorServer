package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.Entities.FragmentRequest;
import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.enums.FragmentRequestStatus;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.repositories.FragmentRequestRepository;
import com.michal5111.fragmentatorServer.utils.Properties;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static com.michal5111.fragmentatorServer.utils.Utils.getMovieExtension;

//@EnableConfigurationProperties(Properties.class)
@Service
public class ConverterService {

    private final FragmentRequestRepository fragmentRequestRepository;
    private final Properties properties;

    public ConverterService(FragmentRequestRepository fragmentRequestRepository, Properties properties) {
        this.fragmentRequestRepository = fragmentRequestRepository;
        this.properties = properties;
    }

    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Logger logger = LoggerFactory.getLogger(ConverterService.class);

    private String nameGenerator(Movie movie, List<Line> lines) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(movie.getFileName());
        lines.forEach(line -> {
            stringBuilder.append(line.getTextLines());
            stringBuilder.append(line.getNumber());
            stringBuilder.append(line.getTimeFrom());
            stringBuilder.append(line.getTimeTo());
        });
        return String.valueOf(stringBuilder.toString().hashCode());
    }

    private String nameGenerator(FragmentRequest fragmentRequest, List<Line> lines) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(fragmentRequest.getMovie().getFileName());
        stringBuilder.append(fragmentRequest.getStartOffset());
        stringBuilder.append(fragmentRequest.getStopOffset());
        lines.forEach(line -> {
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

    public String getSnapshot(Line line) throws IOException, InterruptedException, MovieNotFoundException {
        Movie movie = line.getSubtitles().getMovie();
        String filename = nameGenerator(movie, Collections.singletonList(line));
        File file = new File(properties.getImageCache() + File.separator + filename+"."+properties.getConversionImageFormat());
        if (file.exists()) {
            return filename+"."+properties.getConversionImageFormat();
        }
        movie.setExtension(getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
        String timeString = dateTimeFormatter.format(line.getTimeFrom());
        File tempSubtitles = Utils.createTempSubtitles(line);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-hide_banner",
                "-n",
                "-ss", timeString,
                "-i", movie.getPath()+File.separator+movie.getFileName()+movie.getExtension(),
                "-vf","subtitles="+tempSubtitles.getPath(),
                "-frames:v", "1",
                properties.getImageCache() + File.separator + filename+"."+properties.getConversionImageFormat()
        );
        Process process = processBuilder.start();
        final BufferedReader snapshotReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        snapshotReader.lines().forEach(l -> logger.debug(l));
        process.waitFor();
        logger.debug("Exit value: "+ process.exitValue());
        tempSubtitles.delete();
        return filename+"."+properties.getConversionImageFormat();
    }

    public Flux<ServerSentEvent<String>> convertFragment(FragmentRequest fragmentRequest, List<Line> lines) throws IOException, MovieNotFoundException {
        Movie movie = fragmentRequest.getMovie();
        String fragmentName = nameGenerator(fragmentRequest, lines) + "." + properties.getConversionVideoFormat();
        String fragmentPathString = properties.getVideoCache() + File.separator +fragmentName;
        Path fragmentPath = Path.of(fragmentPathString);

        if (fragmentPath.toFile().exists()) {
            logger.debug("File exist");
            return Flux.create(emitter -> {
                fragmentRequest.setStatus(FragmentRequestStatus.COMPLETE);
                fragmentRequestRepository.save(fragmentRequest);
                emitter.next(ServerSentEvent.<String>builder().event("complete").id("2").data(fragmentName).build());
                emitter.complete();
            });
        } else {
            logger.debug("File not exist "+fragmentPathString);
        }

        movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath()),movie.getFileName()));
        logger.debug("Movie extension: " + movie.getExtension());
        LocalTime time = fragmentRequest.getStartLine().getTimeFrom().minusSeconds(1)
                .plusNanos((Double.valueOf(fragmentRequest.getStartOffset()*1000000000.0)).longValue());
        double to = Utils.calculateDuration(fragmentRequest.getStartLine().getTimeFrom(),fragmentRequest.getStopLine().getTimeTo(),fragmentRequest.getStartOffset(),fragmentRequest.getStopOffset());
        logger.debug("Time lenght: " + to);
        String timeString2 = dateTimeFormatter.format(time);
        logger.debug("Start Time: " + timeString2);
        logger.debug("Converting subtitles...");
        File tempSubtitlesFile = File.createTempFile("temp", ".srt");
        ProcessBuilder subtitlesProcessBuilder = new ProcessBuilder();
        subtitlesProcessBuilder.command(
                "ffmpeg",
                "-y",
                "-i", movie.getPath()+ File.separator + movie.getSubtitles().getFilename(),
                "-ss", timeString2,
                tempSubtitlesFile.getPath()
        );
        Process subtitleProcess = subtitlesProcessBuilder.redirectErrorStream(true).start();
        final BufferedReader SubtitleReader = new BufferedReader(new InputStreamReader(subtitleProcess.getInputStream()));
        SubtitleReader.lines().forEach(line -> logger.debug(line));
        try {
            subtitleProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.debug("Done converting subtitles. Exit status: " + subtitleProcess.exitValue());
        if (subtitleProcess.exitValue() != 0) {
            fragmentRequest.setStatus(FragmentRequestStatus.ERROR);
            fragmentRequest.setErrorMessage("Error in conversion of subtitles!");
            fragmentRequestRepository.save(fragmentRequest);
            throw new IllegalStateException("Error in conversion of subtitles!");
        }
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-y",
                "-ss", timeString2,
                "-i", movie.getPath()+File.separator+movie.getFileName()+movie.getExtension(),
                "-ss", "00:00:01",
                "-t", String.format(Locale.US, "%.3f", to),
                "-acodec", properties.getConversionAudioCodec(),
                "-vcodec", properties.getConversionVideoCodec(),
                "-preset", "veryslow",
                "-vf", "subtitles=" + tempSubtitlesFile.getPath(),
                properties.getVideoCache() + File.separator + fragmentName
        );
        processBuilder.redirectErrorStream(true);

        fragmentRequest.setStatus(FragmentRequestStatus.CONVERTING);
        fragmentRequestRepository.save(fragmentRequest);

        Process process = processBuilder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Stream<String> stringStream = reader.lines().peek(s -> {
            if (s.contains("Conversion failed!")) {
                File fragmentFile = new File(properties.getVideoCache() + File.separator+fragmentName);
                fragmentFile.delete();
                fragmentRequest.setErrorMessage("Conversion failed!");
                fragmentRequest.setStatus(FragmentRequestStatus.ERROR);
                fragmentRequestRepository.save(fragmentRequest);
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
            fragmentRequest.setStatus(FragmentRequestStatus.COMPLETE);
            fragmentRequest.setResultFileName(fragmentName);
            fragmentRequestRepository.save(fragmentRequest);
            tempSubtitlesFile.delete();
            emmiter.next(ServerSentEvent.<String>builder().event("complete").id("2").data(fragmentName).build());
            emmiter.complete();
        });
        return toEvent.concatWith(percent.concatWith(complete))
                .doOnError(s -> ServerSentEvent.builder(s).event("error").data(s).id("2").build());
    }
}
