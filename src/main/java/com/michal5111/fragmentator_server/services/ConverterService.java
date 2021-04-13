package com.michal5111.fragmentator_server.services;

import com.michal5111.fragmentator_server.domain.FragmentRequest;
import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Movie;
import com.michal5111.fragmentator_server.domain.Subtitles;
import com.michal5111.fragmentator_server.dto.LineDto;
import com.michal5111.fragmentator_server.enums.FFMPEGConversionType;
import com.michal5111.fragmentator_server.enums.FragmentRequestStatus;
import com.michal5111.fragmentator_server.exceptions.FFMPEGException;
import com.michal5111.fragmentator_server.exceptions.InvalidFFMPEGPropertiesException;
import com.michal5111.fragmentator_server.ffmpeg.FFMPEGProperties;
import com.michal5111.fragmentator_server.ffmpeg.FFMPEGWrapper;
import com.michal5111.fragmentator_server.repositories.FragmentRequestRepository;
import com.michal5111.fragmentator_server.utils.Properties;
import com.michal5111.fragmentator_server.utils.Utils;
import com.michal5111.fragmentator_server.writers.SRTSubtitlesWriter;
import com.michal5111.fragmentator_server.writers.SubtitlesWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConverterService {

    private final FragmentRequestRepository fragmentRequestRepository;
    private final Properties properties;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @PersistenceContext
    private final EntityManager entityManager = null;

    public ConverterService(FragmentRequestRepository fragmentRequestRepository, Properties properties) {
        this.fragmentRequestRepository = fragmentRequestRepository;
        this.properties = properties;
    }

    private void onComplete(FragmentRequest fragmentRequest) {
        fragmentRequest.setStatus(FragmentRequestStatus.COMPLETE);
        fragmentRequestRepository.save(fragmentRequest);
        try {
            fragmentRequest.getTempFiles().clear();
        } catch (IOException e) {
            log.warn("Error in deleting temp files! {}", e.getMessage());
        }
    }

    private void onError(Throwable e, FragmentRequest fragmentRequest) {
        fragmentRequest.getTempFiles().add(
                new File(properties.getVideoCache(), fragmentRequest.getResultFileName())
        );
        log.error(e.getMessage());
        fragmentRequest.setStatus(FragmentRequestStatus.ERROR);
        fragmentRequest.setErrorMessage(e.getMessage());
        fragmentRequestRepository.save(fragmentRequest);
        try {
            fragmentRequest.getTempFiles().clear();
        } catch (IOException ex) {
            log.warn("Error in deleting temp files! {}", ex.getMessage());
        }
    }

    public Flux<ConversionStatus> convertFragment(FragmentRequest fragmentRequest) {
        return startConvertingFragment(fragmentRequest)
                .doOnComplete(() -> onComplete(fragmentRequest))
                .doOnError(e -> onError(e, fragmentRequest));
    }

    private Flux<ConversionStatus> startConvertingFragment(FragmentRequest fragmentRequest) {
        Movie movie = fragmentRequest.getMovie();
        fragmentRequest.setResultFileName(nameGenerator(fragmentRequest));
        String startTimeString = getStartTimeString(fragmentRequest);
        Double timeLength = getTimeLength(fragmentRequest);
        Path outputFilePath = Path.of(properties.getVideoCache(), fragmentRequest.getResultFileName());
        if (outputFilePath.toFile().exists()) {
            return Flux.just(new ConversionStatus(timeLength, fragmentRequest.getResultFileName(), EventType.COMPLETE));
        }
        File tempSubtitlesFile = createTempSubtitles(fragmentRequest, startTimeString).block();
        fragmentRequest.getTempFiles().add(tempSubtitlesFile);

        Path inputFilePath = Paths.get(movie.getPath(), movie.getFileName() + "." + movie.getExtension());
        log.debug("Input file: {}", inputFilePath);
        log.debug("Movie extension: {}", movie.getExtension());
        log.debug("Time length: {}", timeLength);
        log.debug("Start Time: {}", startTimeString);

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

        log.debug("Properties: {}", ffmpegProperties);

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
                .doOnNext(log::debug)
                .map(s -> new ConversionStatus(null, s, EventType.LOG));
        Mono<ConversionStatus> completeMono = Mono.just(
                new ConversionStatus(null, fragmentRequest.getResultFileName(), EventType.COMPLETE)
        );
        return Flux.concat(timeLengthMono, percentFlux, completeMono);
    }

    private void replaceLinesWithEdits(FragmentRequest fragmentRequest, Map<Integer, LineDto> lines) {
        fragmentRequest
                .getLineEdits()
                .forEach(lineEdit -> {
                    Line line = lineEdit.getLine();
                    lines.get(line.getNumber()).setTextLines(lineEdit.getText());
                    log.info("zamieniam linię nr. {} na tekst {}", line.getNumber(), lineEdit.getText());
                });
    }

    private Mono<File> createTempSubtitles(FragmentRequest fragmentRequest, String startTimeString) {
        log.debug("Converting subtitles...");
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
            Map<Integer, LineDto> lineDtoList = subtitles
                    .getLines()
                    .stream()
                    .map(LineDto::new)
                    .collect(Collectors.toMap(LineDto::getNumber, lineDto -> lineDto));
            replaceLinesWithEdits(fragmentRequest, lineDtoList);
            SubtitlesWriter subtitlesWriter = new SRTSubtitlesWriter();
            try {
                inputFile = subtitlesWriter.write(lineDtoList);
            } catch (IOException e) {
                return Mono.error(e);
            }
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
                .doOnNext(log::debug)
                .then(Mono.fromFuture(ffmpegWrapper.getProcess().onExit()))
                .map(Process::exitValue)
                .flatMap(returnValue -> {
                    if (returnValue != 0) {
                        return Mono.error(new FFMPEGException("Return value = " + returnValue));
                    }
                    return Mono.just(returnValue);
                })
                .doOnNext(returnValue -> log.debug("Done converting subtitles. Exit status: {}", returnValue))
                .map(returnValue -> outputFile)
                .doOnError(e -> {
                    log.error(e.getMessage(), e);
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
        fragmentRequest.getLineEdits().forEach(lineEdit -> stringBuilder.append(lineEdit.getText()));
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
                        (Double.valueOf(fragmentRequest.getStartOffset() * 1000000000.0)).longValue()
                );
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
        File file = new File(properties.getImageCache(), filename + "." + properties.getConversionImageFormat());
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
                .doOnNext(log::debug)
                .doOnComplete(() -> {
                    try {
                        Files.deleteIfExists(tempSubtitles.toPath());
                    } catch (IOException e) {
                        log.warn("Error in deleting file {}", tempSubtitles.getName());
                    }
                })
                .then(Mono.fromFuture(ffmpegWrapper.getProcess().onExit()))
                .map(Process::exitValue)
                .flatMap(returnValue -> {
                    if (returnValue != 0) {
                        return Mono.error(new FFMPEGException("Return value = " + returnValue));
                    }
                    return Mono.just(returnValue);
                })
                .doOnNext(returnValue -> log.debug("Done converting snapshot. Exit status: {}", returnValue))
                .map(returnValue -> outputFilePath.toFile());
    }

    private static class EventType {
        private static final String TO = "to";
        private static final String LOG = "log";
        private static final String COMPLETE = "complete";
    }

    @AllArgsConstructor
    @Data
    public static class ConversionStatus {
        private Double timeLength;
        private String logLine;
        private String eventType;
    }
}
