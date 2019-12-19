package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.domain.FragmentRequest;
import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Movie;
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
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private Logger logger = LoggerFactory.getLogger(ConverterService.class);

    public ConverterService(FragmentRequestRepository fragmentRequestRepository, Properties properties) {
        this.fragmentRequestRepository = fragmentRequestRepository;
        this.properties = properties;
    }

    private static class EventTypes {
        private static final String TO = "to";
        private static final String LOG = "log";
        private static final String COMPLETE = "complete";
        private static final String ERROR = "error";
    }

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
        return String.valueOf(stringBuilder.toString().hashCode())
                .concat(".")
                .concat(properties.getConversionVideoFormat());
    }

    private String getStartTimeString(FragmentRequest fragmentRequest) {
        LocalTime time = fragmentRequest
                .getStartLine()
                .getTimeFrom()
                .minusSeconds(1)
                .plusNanos(
                        (
                                Double.valueOf(fragmentRequest.getStartOffset() * 1000000000.0)
                        ).longValue());
        return dateTimeFormatter.format(time);
    }

    private Double getTimeLength(FragmentRequest fragmentRequest) {
        return Utils.calculateDuration(
                fragmentRequest.getStartLine().getTimeFrom(),
                fragmentRequest.getStopLine().getTimeTo(),
                fragmentRequest.getStartOffset(),
                fragmentRequest.getStopOffset()
        );
    }

    private File createTempSubtitles(
            FragmentRequest fragmentRequest,
            String startTimeString
    ) throws IOException, InterruptedException {
        logger.debug("Converting subtitles...");
        Movie movie = fragmentRequest.getMovie();
        File tempSubtitlesFile = File.createTempFile("temp", ".srt");
        File inputFile = new File(movie.getPath() + File.separator + movie.getSubtitles().getFilename());
        if (!fragmentRequest.getLineEdits().isEmpty()) {
            File preTempSubtitlesFile = File.createTempFile("preTemp", ".srt");
            fragmentRequest.getMovie().getSubtitles().getLines().forEach(line ->
                    fragmentRequest.getLineEdits().forEach(lineEdit -> {
                        if (line.getId().equals(lineEdit.getLine().getId())) {
                            logger.debug("Replacing " + line.getId() + "\n" + line.getTextLines() + "\nto\n" + lineEdit.getId() + "\n" + lineEdit.getText());
                            line.setTextLines(lineEdit.getText());
                        }
                    }));
            fragmentRequest.getMovie().getSubtitles().saveToFile(preTempSubtitlesFile);
            inputFile = preTempSubtitlesFile;
        }
        ProcessBuilder subtitlesProcessBuilder = new ProcessBuilder();
        subtitlesProcessBuilder.command(
                "ffmpeg",
                "-y",
                "-i", inputFile.getAbsolutePath(),
                "-ss", startTimeString,
                tempSubtitlesFile.getPath()
        );
        Process subtitleProcess = subtitlesProcessBuilder.redirectErrorStream(true).start();
        try (final BufferedReader subtitleReader = new BufferedReader(
                new InputStreamReader(subtitleProcess.getInputStream()))) {
            subtitleReader.lines().forEach(line -> logger.debug(line));
        }
        subtitleProcess.waitFor();
        logger.debug("Done converting subtitles. Exit status: " + subtitleProcess.exitValue());
        if (subtitleProcess.exitValue() != 0) {
            fragmentRequest.setStatus(FragmentRequestStatus.ERROR);
            fragmentRequest.setErrorMessage("Error in conversion of subtitles!");
            fragmentRequestRepository.save(fragmentRequest);
            throw new IllegalStateException("Error in conversion of subtitles!");
        }
        return tempSubtitlesFile;
    }

    private ProcessBuilder prepareProcess(
            FragmentRequest fragmentRequest,
            String startTimeString,
            Double timeLength,
            File tempSubtitlesFile,
            String fragmentName
    ) {
        Movie movie = fragmentRequest.getMovie();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-y",
                "-ss", startTimeString,
                "-i", movie.getPath() + File.separator + movie.getFileName() + movie.getExtension(),
                "-ss", "00:00:01",
                "-t", String.format(Locale.US, "%.3f", timeLength),
                "-acodec", properties.getConversionAudioCodec(),
                "-vcodec", properties.getConversionVideoCodec(),
                "-preset", "veryslow",
                "-vf", "subtitles=" + tempSubtitlesFile.getPath(),
                properties.getVideoCache() + File.separator + fragmentName
        );
        processBuilder.redirectErrorStream(true);
        return processBuilder;
    }

    public String getSnapshot(Line line) throws IOException, InterruptedException, MovieNotFoundException {
        Movie movie = line.getSubtitles().getMovie();
        String filename = nameGenerator(movie, Collections.singletonList(line));
        File file = new File(properties.getImageCache() + File.separator + filename + "." + properties.getConversionImageFormat());
        if (file.exists()) {
            return filename + "." + properties.getConversionImageFormat();
        }
        movie.setExtension(getMovieExtension(Paths.get(movie.getPath()), movie.getFileName()));
        String timeString = dateTimeFormatter.format(line.getTimeFrom());
        File tempSubtitles = Utils.createTempSubtitles(line);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-hide_banner",
                "-n",
                "-ss", timeString,
                "-i", movie.getPath() + File.separator + movie.getFileName() + movie.getExtension(),
                "-vf", "subtitles=" + tempSubtitles.getPath(),
                "-frames:v", "1",
                properties.getImageCache() + File.separator + filename + "." + properties.getConversionImageFormat()
        );
        Process process = processBuilder.start();
        try (final BufferedReader snapshotReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            snapshotReader.lines().forEach(l -> logger.debug(l));
        }
        process.waitFor();
        logger.debug("Exit value: " + process.exitValue());
        tempSubtitles.delete();
        return filename + "." + properties.getConversionImageFormat();
    }

    public Flux<ServerSentEvent<String>> convertFragment(
            FragmentRequest fragmentRequest,
            List<Line> lines
    ) throws IOException, MovieNotFoundException, InterruptedException {
        Movie movie = fragmentRequest.getMovie();
        String fragmentName = nameGenerator(fragmentRequest, lines);
        String fragmentPathString = properties.getVideoCache() + File.separator + fragmentName;
        String startTimeString = getStartTimeString(fragmentRequest);
        Double timeLength = getTimeLength(fragmentRequest);
        File tempSubtitlesFile = createTempSubtitles(fragmentRequest, startTimeString);
        Path fragmentPath = Path.of(fragmentPathString);

        if (fragmentPath.toFile().exists()) {
            logger.debug("File exist");
            return Flux.create(emitter -> {
                fragmentRequest.setStatus(FragmentRequestStatus.COMPLETE);
                fragmentRequestRepository.save(fragmentRequest);
                emitter.next(ServerSentEvent.<String>builder()
                        .event(EventTypes.COMPLETE).id(fragmentRequest.getId().toString()).data(fragmentName).build());
                emitter.complete();
            });
        } else {
            logger.debug("File not exist " + fragmentPathString);
        }

        movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath()), movie.getFileName()));
        logger.debug("Movie extension: " + movie.getExtension());
        logger.debug("Time lenght: " + timeLength);
        logger.debug("Start Time: " + startTimeString);

        ProcessBuilder processBuilder = prepareProcess(
                fragmentRequest,
                startTimeString,
                timeLength,
                tempSubtitlesFile,
                fragmentName
        );

        fragmentRequest.setStatus(FragmentRequestStatus.CONVERTING);
        fragmentRequestRepository.save(fragmentRequest);

        Process process = processBuilder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        Stream<String> stringStream = reader.lines().peek(s -> {
            if (s.contains("Conversion failed!")) {
                File fragmentFile = new File(properties.getVideoCache() + File.separator + fragmentName);
                fragmentFile.delete();
                fragmentRequest.setErrorMessage("Conversion failed!");
                fragmentRequest.setStatus(FragmentRequestStatus.ERROR);
                fragmentRequestRepository.save(fragmentRequest);
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                throw new IllegalStateException("Conversion failed!");
            }
        });
        Flux<ServerSentEvent<String>> toEvent = Flux.create(emitter -> {
            emitter.next(ServerSentEvent.<String>builder()
                    .event(EventTypes.TO).id(fragmentRequest.getId().toString()).data(String.valueOf(timeLength)).build());
            emitter.complete();
        });
        Flux<ServerSentEvent<String>> percent = Flux.fromStream(stringStream)
                .doOnNext(s -> logger.debug(s))
                .map(s -> ServerSentEvent.builder(s)
                        .event(EventTypes.LOG).data(s).id(fragmentRequest.getId().toString()).build());
        Flux<ServerSentEvent<String>> complete = Flux.create(emitter -> {
            fragmentRequest.setStatus(FragmentRequestStatus.COMPLETE);
            fragmentRequest.setResultFileName(fragmentName);
            fragmentRequestRepository.save(fragmentRequest);
            tempSubtitlesFile.delete();
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            emitter.next(ServerSentEvent.<String>builder()
                    .event(EventTypes.COMPLETE).id(fragmentRequest.getId().toString()).data(fragmentName).build());
            emitter.complete();
        });
        return toEvent.concatWith(percent.concatWith(complete))
                .doOnError(s -> ServerSentEvent.builder(s)
                        .event(EventTypes.ERROR).data(s).id(fragmentRequest.getId().toString()).build());
    }
}
