package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.domain.FragmentRequest;
import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Movie;
import com.michal5111.fragmentatorServer.enums.FFMPEGConversionType;
import com.michal5111.fragmentatorServer.enums.FragmentRequestStatus;
import com.michal5111.fragmentatorServer.exceptions.InvalidFFMPEGPropertiesException;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import com.michal5111.fragmentatorServer.exceptions.SubtitlesNotFoundException;
import com.michal5111.fragmentatorServer.ffmpeg.FFMPEGProperties;
import com.michal5111.fragmentatorServer.ffmpeg.FFMPEGWrapper;
import com.michal5111.fragmentatorServer.repositories.FragmentRequestRepository;
import com.michal5111.fragmentatorServer.utils.Properties;
import com.michal5111.fragmentatorServer.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.michal5111.fragmentatorServer.utils.Utils.getMovieExtension;

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

    private void onComplete(
            FragmentRequest fragmentRequest,
            File tempSubtitlesFile,
            String fragmentName) {
        fragmentRequest.setStatus(FragmentRequestStatus.COMPLETE);
        fragmentRequest.setResultFileName(fragmentName);
        fragmentRequestRepository.save(fragmentRequest);
        if (!tempSubtitlesFile.delete()) {
            logger.error("Error in deleting file " + tempSubtitlesFile.getName());
        }
    }

    private void onError(Throwable e, FragmentRequest fragmentRequest, String fragmentName) {
        File fragmentFile = new File(properties.getVideoCache() + File.separator + fragmentName);
        if (!fragmentFile.delete()) {
            logger.error("Error in deleting file " + fragmentFile.getName());
        }
        logger.error(e.getMessage());
        fragmentRequest.setStatus(FragmentRequestStatus.ERROR);
        fragmentRequest.setErrorMessage(e.getMessage());
        fragmentRequestRepository.save(fragmentRequest);
    }

    public Flux<ConversionStatus> convertFragment(
            FragmentRequest fragmentRequest,
            List<Line> lines
    ) throws IOException, MovieNotFoundException, InterruptedException, SubtitlesNotFoundException, InvalidFFMPEGPropertiesException {
        Movie movie = fragmentRequest.getMovie();
        String fragmentName = nameGenerator(fragmentRequest, lines);
        String fragmentPathString = properties.getVideoCache() + File.separator + fragmentName;
        String startTimeString = getStartTimeString(fragmentRequest);
        Double timeLength = getTimeLength(fragmentRequest);
        File tempSubtitlesFile = createTempSubtitles(fragmentRequest, startTimeString);
        Path outputFilePath = Path.of(fragmentPathString);

        if (outputFilePath.toFile().exists()) {
            logger.debug("File exist");
            return Mono.just(new ConversionStatus(timeLength, fragmentName, EventType.COMPLETE)
            ).flux().doOnComplete(() -> onComplete(fragmentRequest, tempSubtitlesFile, fragmentName));
        } else {
            logger.debug("File not exist " + fragmentPathString);
        }

        // TODO: 09.01.2020 Zrobić coś z tym set extension
        movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath()), movie.getFileName()));

        Path inputFilePath = Paths.get(movie.getPath(), movie.getFileName() + movie.getExtension());

        movie.setExtension(Utils.getMovieExtension(Paths.get(movie.getPath()), movie.getFileName()));
        logger.debug("Movie extension: " + movie.getExtension());
        logger.debug("Time lenght: " + timeLength);
        logger.debug("Start Time: " + startTimeString);

        FFMPEGProperties ffmpegProperties = FFMPEGProperties.builder()
                .conversionType(FFMPEGConversionType.VIDEO)
                .inputFilePath(inputFilePath)
                .outputFilePath(outputFilePath)
                .startTime(startTimeString)
                .timeLength(timeLength)
                .audioCodec(properties.getConversionAudioCodec())
                .videoCodec(properties.getConversionVideoCodec())
                .subtitlesPath(tempSubtitlesFile.toPath())
                .build();

        logger.debug("Properties:" + ffmpegProperties.toString());

        FFMPEGWrapper ffmpegWrapper = new FFMPEGWrapper();

        ffmpegWrapper.setProperties(ffmpegProperties);

        ffmpegWrapper.createProcessBuilder();

        fragmentRequest.setStatus(FragmentRequestStatus.CONVERTING);
        fragmentRequestRepository.save(fragmentRequest);

        Stream<String> stringStream = ffmpegWrapper.getInputStream();
        Mono<ConversionStatus> timeLengthMono = Mono.just(
                new ConversionStatus(timeLength, null, EventType.TO)
        );
        Flux<ConversionStatus> percentFlux = Flux.fromStream(stringStream)
                .doOnNext(s -> logger.debug(s))
                .map(s -> new ConversionStatus(null, s, EventType.LOG));
        Mono<ConversionStatus> completeMono = Mono.just(
                new ConversionStatus(null, fragmentName, EventType.COMPLETE)
        );
        return Flux.concat(timeLengthMono, percentFlux, completeMono)
                .doOnComplete(() -> onComplete(fragmentRequest, tempSubtitlesFile, fragmentName))
                .doOnError(e -> onError(e, fragmentRequest, fragmentName));

    }

    private static class EventType {
        private static final String TO = "to";
        private static final String LOG = "log";
        private static final String COMPLETE = "complete";
        private static final String ERROR = "error";
    }

    private File createTempSubtitles(
            FragmentRequest fragmentRequest,
            String startTimeString
    ) throws IOException, InterruptedException, SubtitlesNotFoundException, InvalidFFMPEGPropertiesException {
        logger.debug("Converting subtitles...");
        Movie movie = fragmentRequest.getMovie();
        File tempSubtitlesFile = File.createTempFile("temp", ".srt");
        File inputFile = movie.getSubtitles().getSubtitleFile();
        if (!inputFile.exists()) {
            throw new SubtitlesNotFoundException("Subtitles file not found!");
        }
        if (!fragmentRequest.getLineEdits().isEmpty()) {
            File preTempSubtitlesFile = File.createTempFile("preTemp", ".srt");
            fragmentRequest.getMovie().getSubtitles().getLines().forEach(line ->
                    fragmentRequest.getLineEdits().forEach(lineEdit -> {
                        if (line.getId().equals(lineEdit.getLine().getId())) {
                            logger.debug("Replacing "
                                    + line.getId()
                                    + "\n" + line.getTextLines()
                                    + "\nto\n" + lineEdit.getId()
                                    + "\n" + lineEdit.getText());
                            line.setTextLines(lineEdit.getText());
                        }
                    }));
            fragmentRequest.getMovie().getSubtitles().saveToFile(preTempSubtitlesFile);
            inputFile = preTempSubtitlesFile;
        }

        FFMPEGProperties ffmpegProperties = FFMPEGProperties.builder()
                .conversionType(FFMPEGConversionType.SUBTITLES)
                .inputFilePath(inputFile.toPath())
                .outputFilePath(tempSubtitlesFile.toPath())
                .startTime(startTimeString)
                .build();

        FFMPEGWrapper ffmpegWrapper = new FFMPEGWrapper();

        ffmpegWrapper.setProperties(ffmpegProperties);

        ffmpegWrapper.createProcessBuilder();

        Stream<String> stringStream = ffmpegWrapper.getInputStream();

        stringStream.forEach(line -> logger.debug(line));

        ffmpegWrapper.getProcess().waitFor();
        logger.debug("Done converting subtitles. Exit status: " + ffmpegWrapper.getProcess().exitValue());
        if (ffmpegWrapper.getProcess().exitValue() != 0) {
            fragmentRequest.setStatus(FragmentRequestStatus.ERROR);
            fragmentRequest.setErrorMessage("Error in conversion of subtitles!");
            fragmentRequestRepository.save(fragmentRequest);
            throw new IllegalStateException("Error in conversion of subtitles!");
        }
        return tempSubtitlesFile;
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

    public String getSnapshot(Line line) throws InterruptedException, MovieNotFoundException, IOException, InvalidFFMPEGPropertiesException {
        Movie movie = line.getSubtitles().getMovie();
        String filename = nameGenerator(movie, Collections.singletonList(line));
        File file = new File(properties.getImageCache() + File.separator + filename + "." + properties.getConversionImageFormat());
        if (file.exists()) {
            return filename + "." + properties.getConversionImageFormat();
        }
        try {
            movie.setExtension(getMovieExtension(Paths.get(movie.getPath()), movie.getFileName()));
        } catch (IOException e) {
            throw new MovieNotFoundException("Movie file not Found!");
        }
        String timeString = dateTimeFormatter.format(line.getTimeFrom());
        File tempSubtitles = Utils.createTempSubtitles(line);

        Path inputFilePath = Paths.get(movie.getPath(), movie.getFileName() + movie.getExtension());
        Path outputFilePath = Paths.get(properties.getImageCache(), filename + "." + properties.getConversionImageFormat());
        Path tempSubtitlesPath = Paths.get(tempSubtitles.getPath());

        FFMPEGProperties ffmpegProperties = FFMPEGProperties.builder()
                .conversionType(FFMPEGConversionType.IMAGE)
                .inputFilePath(inputFilePath)
                .outputFilePath(outputFilePath)
                .subtitlesPath(tempSubtitlesPath)
                .startTime(timeString)
                .build();

        FFMPEGWrapper ffmpegWrapper = new FFMPEGWrapper();

        ffmpegWrapper.setProperties(ffmpegProperties);

        ffmpegWrapper.createProcessBuilder();

        Stream<String> stringStream = ffmpegWrapper.getInputStream();

        stringStream.forEach(l -> logger.debug(l));
        ffmpegWrapper.getProcess().waitFor();
        logger.debug("Exit value: " + ffmpegWrapper.getProcess().exitValue());
        if (!tempSubtitles.delete()) {
            logger.error("Error in deleting file " + tempSubtitles.getName());
        }
        return filename + "." + properties.getConversionImageFormat();
    }

    public static class ConversionStatus {
        public Double timeLength;
        public String logLine;
        public String eventType;

        public ConversionStatus(Double timeLength, String logLine, String eventType) {
            this.timeLength = timeLength;
            this.logLine = logLine;
            this.eventType = eventType;
        }
    }
}
