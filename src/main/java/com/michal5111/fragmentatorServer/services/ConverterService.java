package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.domain.FragmentRequest;
import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Movie;
import com.michal5111.fragmentatorServer.domain.Subtitles;
import com.michal5111.fragmentatorServer.enums.FFMPEGConversionType;
import com.michal5111.fragmentatorServer.enums.FragmentRequestStatus;
import com.michal5111.fragmentatorServer.exceptions.InvalidFFMPEGPropertiesException;
import com.michal5111.fragmentatorServer.ffmpeg.FFMPEGProperties;
import com.michal5111.fragmentatorServer.ffmpeg.FFMPEGWrapper;
import com.michal5111.fragmentatorServer.repositories.FragmentRequestRepository;
import com.michal5111.fragmentatorServer.utils.Properties;
import com.michal5111.fragmentatorServer.utils.Utils;
import lombok.AllArgsConstructor;
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

@Service
public class ConverterService {

    private final FragmentRequestRepository fragmentRequestRepository;
    private final Properties properties;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final Logger logger = LoggerFactory.getLogger(ConverterService.class);

    public ConverterService(FragmentRequestRepository fragmentRequestRepository, Properties properties) {
        this.fragmentRequestRepository = fragmentRequestRepository;
        this.properties = properties;
    }

    private void onComplete(
            FragmentRequest fragmentRequest) {
        fragmentRequest.setStatus(FragmentRequestStatus.COMPLETE);
        fragmentRequestRepository.save(fragmentRequest);
        fragmentRequest.getTempFiles().clear();
    }

    private void onError(Throwable e, FragmentRequest fragmentRequest) {
        fragmentRequest.getTempFiles().add(
                new File(properties.getVideoCache() + File.separator + fragmentRequest.getResultFileName())
        );
        logger.error(e.getMessage());
        fragmentRequest.setStatus(FragmentRequestStatus.ERROR);
        fragmentRequest.setErrorMessage(e.getMessage());
        fragmentRequestRepository.save(fragmentRequest);
        fragmentRequest.getTempFiles().clear();
    }

    public Flux<ConversionStatus> convertFragment(FragmentRequest fragmentRequest) {
        return startConvertingFragment(fragmentRequest)
                .doOnComplete(() -> onComplete(fragmentRequest))
                .doOnError(e -> onError(e, fragmentRequest));
    }

    private Flux<ConversionStatus> startConvertingFragment(FragmentRequest fragmentRequest) {
        Movie movie = fragmentRequest.getMovie();
        fragmentRequest.setResultFileName(nameGenerator(fragmentRequest));
        String fragmentPathString = properties.getVideoCache() + File.separator + fragmentRequest.getResultFileName();
        String startTimeString = getStartTimeString(fragmentRequest);
        Double timeLength = getTimeLength(fragmentRequest);
        File tempSubtitlesFile = createTempSubtitles(fragmentRequest, startTimeString).block();
        fragmentRequest.getTempFiles().add(tempSubtitlesFile);
        Path outputFilePath = Path.of(fragmentPathString);

        if (outputFilePath.toFile().exists()) {
            logger.debug("File exists");
            return Mono.just(new ConversionStatus(timeLength, fragmentRequest.getResultFileName(), EventType.COMPLETE)).flux();
        } else {
            logger.debug("File does not exists " + fragmentPathString);
        }
        Path inputFilePath = Paths.get(movie.getPath(), movie.getFileName() + "." + movie.getExtension());
        logger.debug("Input file:" + inputFilePath);
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

        FFMPEGWrapper ffmpegWrapper = new FFMPEGWrapper(ffmpegProperties);

        try {
            ffmpegWrapper.prepare();
        } catch (InvalidFFMPEGPropertiesException e) {
            return Flux.error(e);
        }

        fragmentRequest.setStatus(FragmentRequestStatus.CONVERTING);
        fragmentRequestRepository.save(fragmentRequest);

        Mono<ConversionStatus> timeLengthMono = Mono.just(
                new ConversionStatus(timeLength, null, EventType.TO)
        );
        Flux<ConversionStatus> percentFlux = ffmpegWrapper.getInputFlux()
                .doOnNext(logger::debug)
                .map(s -> new ConversionStatus(null, s, EventType.LOG));
        Mono<ConversionStatus> completeMono = Mono.just(
                new ConversionStatus(null, fragmentRequest.getResultFileName(), EventType.COMPLETE)
        );
        return Flux.concat(timeLengthMono, percentFlux, completeMono);
    }

    private void replaceLinesWithEdits(FragmentRequest fragmentRequest) {
        fragmentRequest
                .getLineEdits()
                .forEach(lineEdit -> {
                    lineEdit.setOriginalText(lineEdit.getLine().getTextLines());
                    lineEdit.getLine().setTextLines(lineEdit.getText());
                });
    }

    private void restoreOriginalLines(FragmentRequest fragmentRequest) {
        fragmentRequest
                .getLineEdits()
                .forEach(lineEdit -> lineEdit.getLine().setTextLines(lineEdit.getOriginalText()));

    }

    private Mono<File> createTempSubtitles(FragmentRequest fragmentRequest, String startTimeString) {
        logger.debug("Converting subtitles...");
        Movie movie = fragmentRequest.getMovie();
        Subtitles subtitles = movie.getSubtitles();
        File outputFile;
        try {
            outputFile = fragmentRequest.getTempFiles().add(
                    File.createTempFile("temp", ".srt")
            );
        } catch (IOException e) {
            return Mono.error(e);
        }
        File inputFile = movie.getSubtitles().getSubtitleFile();
        if (!fragmentRequest.getLineEdits().isEmpty()) {
            File preTempSubtitlesFile;
            try {
                preTempSubtitlesFile = fragmentRequest.getTempFiles().add(
                        File.createTempFile("preTemp", ".srt")
                );
            } catch (IOException e) {
                return Mono.error(e);
            }
            replaceLinesWithEdits(fragmentRequest);
            subtitles.saveToFile(preTempSubtitlesFile);
            restoreOriginalLines(fragmentRequest);
            inputFile = preTempSubtitlesFile;
        }

        FFMPEGProperties ffmpegProperties = FFMPEGProperties.builder()
                .conversionType(FFMPEGConversionType.SUBTITLES)
                .inputFilePath(inputFile.toPath())
                .outputFilePath(outputFile.toPath())
                .startTime(startTimeString)
                .build();

        FFMPEGWrapper ffmpegWrapper = new FFMPEGWrapper(ffmpegProperties);

        try {
            ffmpegWrapper.prepare();
        } catch (InvalidFFMPEGPropertiesException e) {
            return Mono.error(e);
        }

        return ffmpegWrapper.getInputFlux()
                .doOnNext(logger::debug)
                .then(waitForProcess(ffmpegWrapper.getProcess()))
                .doOnNext(returnValue -> logger.debug("Done converting subtitles. Exit status: " + returnValue))
                .map(returnValue -> outputFile)
                .doOnError(e -> {
                    logger.error(e.getMessage());
                    fragmentRequest.setStatus(FragmentRequestStatus.ERROR);
                    fragmentRequest.setErrorMessage("Error in conversion of subtitles! " + e.getMessage());
                    fragmentRequestRepository.save(fragmentRequest);
                });
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

    private String nameGenerator(FragmentRequest fragmentRequest) {
        List<Line> lines = fragmentRequest.getLines();
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
        fragmentRequest.getLineEdits().forEach(lineEdit -> {
            stringBuilder.append(lineEdit.getText());
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

    public Mono<File> getSnapshot(Line line) {
        Movie movie = line.getSubtitles().getMovie();
        String filename = nameGenerator(movie, Collections.singletonList(line));
        File file = new File(properties.getImageCache() + File.separator + filename + "." + properties.getConversionImageFormat());
        if (file.exists()) {
            return Mono.just(file);
        }
        String timeString = dateTimeFormatter.format(line.getTimeFrom());
        File tempSubtitles;
        try {
            tempSubtitles = Utils.createTempSubtitles(line);
        } catch (IOException e) {
            return Mono.error(e);
        }

        Path inputFilePath = Paths.get(movie.getPath(), movie.getFileName() + "." + movie.getExtension());
        Path outputFilePath = Paths.get(properties.getImageCache(), filename + "." + properties.getConversionImageFormat());
        Path tempSubtitlesPath = Paths.get(tempSubtitles.getPath());

        FFMPEGProperties ffmpegProperties = FFMPEGProperties.builder()
                .conversionType(FFMPEGConversionType.IMAGE)
                .inputFilePath(inputFilePath)
                .outputFilePath(outputFilePath)
                .subtitlesPath(tempSubtitlesPath)
                .startTime(timeString)
                .build();

        FFMPEGWrapper ffmpegWrapper = new FFMPEGWrapper(ffmpegProperties);

        try {
            ffmpegWrapper.prepare();
        } catch (InvalidFFMPEGPropertiesException e) {
            return Mono.error(e);
        }

        return ffmpegWrapper.getInputFlux()
                .doOnNext(logger::debug)
                .doOnComplete(() -> {
                    if (!tempSubtitles.delete()) {
                        logger.error("Error in deleting file " + tempSubtitles.getName());
                    }
                })
                .then(waitForProcess(ffmpegWrapper.getProcess()))
                .doOnNext(returnValue -> logger.debug("Done converting snapshot. Exit status: " + returnValue))
                .map(returnValue -> outputFilePath.toFile());
    }

    private Mono<Integer> waitForProcess(Process process) {
        return Mono.defer(() -> {
            try {
                int returnValue = process.waitFor();
                if (returnValue != 0) {
                    return Mono.error(new IllegalStateException("Return value = " + returnValue));
                }
                return Mono.just(returnValue);
            } catch (InterruptedException e) {
                return Mono.error(e);
            }
        });
    }

    private static class EventType {
        private static final String TO = "to";
        private static final String LOG = "log";
        private static final String COMPLETE = "complete";
    }

    @AllArgsConstructor
    public static class ConversionStatus {
        public Double timeLength;
        public String logLine;
        public String eventType;
    }
}
