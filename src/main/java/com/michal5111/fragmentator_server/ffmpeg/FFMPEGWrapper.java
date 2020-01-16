package com.michal5111.fragmentator_server.ffmpeg;

import com.michal5111.fragmentator_server.enums.FFMPEGConversionType;
import com.michal5111.fragmentator_server.exceptions.FFMPEGException;
import com.michal5111.fragmentator_server.exceptions.InvalidFFMPEGPropertiesException;
import lombok.Data;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

@Data
public class FFMPEGWrapper {

    private final FFMPEGProperties properties;

    private ProcessBuilder processBuilder;

    private Process process;

    public FFMPEGWrapper(FFMPEGProperties properties) {
        this.properties = properties;
    }

    private static final String FFMPEG = "ffmpeg";
    private static final String INPUT = "-i";
    private static final String START_TIME = "-ss";
    private static final String TIME_LENGTH = "-t";
    private static final String AUDIO_CODEC = "-acodec";
    private static final String VIDEO_CODEC = "-vcodec";
    private static final String HIDE_BANNER = "-hide_banner";
    private static final String OVERRIDE = "-y";
    private static final String PRESET = "-preset";
    private static final String FILTER = "-vf";

    private ProcessBuilder prepareVideoProcess() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                FFMPEG,
                OVERRIDE,
                START_TIME, properties.getStartTime(),
                INPUT, properties.getInputFilePath().toString(),
                START_TIME, "00:00:01",
                TIME_LENGTH, String.format(Locale.US, "%.3f", properties.getTimeLength()),
                AUDIO_CODEC, properties.getAudioCodec(),
                VIDEO_CODEC, properties.getVideoCodec(),
                PRESET, "veryslow",
                FILTER, "subtitles=" + properties.getSubtitlesPath().toString(),
                HIDE_BANNER,
                properties.getOutputFilePath().toString()
        );
        return pb;
    }

    private ProcessBuilder prepareSubtitlesProcess() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                FFMPEG,
                HIDE_BANNER,
                OVERRIDE,
                INPUT, properties.getInputFilePath().toString(),
                START_TIME, properties.getStartTime(),
                properties.getOutputFilePath().toString()
        );
        return pb;
    }

    private ProcessBuilder prepareImageProcess() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                FFMPEG,
                HIDE_BANNER,
                "-n",
                START_TIME, properties.getStartTime(),
                INPUT, properties.getInputFilePath().toString(),
                FILTER, "subtitles=" + properties.getSubtitlesPath(),
                "-frames:v", "1",
                properties.getOutputFilePath().toString()
        );
        return pb;
    }

    private void checkProperties() throws InvalidFFMPEGPropertiesException {
        checkIfPropertiesNotNull();
        checkConversionType();
        checkInputFile();
        checkOutputFile();
        checkStartTime();
        if (properties.getConversionType() == FFMPEGConversionType.VIDEO
                || properties.getConversionType() == FFMPEGConversionType.IMAGE) {
            checkSubtitles();
            if (properties.getConversionType() == FFMPEGConversionType.VIDEO
                    && (properties.getTimeLength() == null || properties.getTimeLength() <= 0)) {
                throw new InvalidFFMPEGPropertiesException("Invalid time length!");
            }
        }
    }

    public void prepare() throws InvalidFFMPEGPropertiesException {
        checkProperties();
        switch (properties.getConversionType()) {
            case VIDEO:
                processBuilder = prepareVideoProcess();
                break;
            case IMAGE:
                processBuilder = prepareImageProcess();
                break;
            case SUBTITLES:
                processBuilder = prepareSubtitlesProcess();
                break;
            default:
                throw new InvalidFFMPEGPropertiesException("Unknown conversion type");
        }
    }

    public Flux<String> getInputFlux() {
        try {
            process = processBuilder.redirectErrorStream(true).start();
        } catch (IOException e) {
            return Flux.error(e);
        }
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );
        return Flux.fromStream(reader.lines())
                .doOnNext(s -> {
                    try {
                        if (s.contains("Conversion failed!")) {
                            throw new FFMPEGException("Conversion failed!");
                        }
                        if (s.contains("Invalid data found when processing input")) {
                            throw new FFMPEGException("Invalid data found when processing input");
                        }
                    } catch (FFMPEGException e) {
                        Flux.error(e);
                    }
                });
    }

    private void checkIfPropertiesNotNull() throws InvalidFFMPEGPropertiesException {
        if (properties == null) {
            throw new InvalidFFMPEGPropertiesException("Properties cannot be null!");
        }
    }

    private void checkConversionType() throws InvalidFFMPEGPropertiesException {
        if (properties.getConversionType() == null) {
            throw new InvalidFFMPEGPropertiesException("Conversion type cannot be null!");
        }
    }

    private void checkInputFile() throws InvalidFFMPEGPropertiesException {
        if (properties.getInputFilePath() == null) {
            throw new InvalidFFMPEGPropertiesException("Input path cannot be null!");
        }
        if (!properties.getInputFilePath().toFile().exists()) {
            throw new InvalidFFMPEGPropertiesException("Input file does not exists!");
        }
        if (!properties.getInputFilePath().toFile().canRead()) {
            throw new InvalidFFMPEGPropertiesException("Cannot read input file!");
        }
    }

    private void checkOutputFile() throws InvalidFFMPEGPropertiesException {
        if (properties.getOutputFilePath() == null) {
            throw new InvalidFFMPEGPropertiesException("Output path cannot be null!");
        }
    }

    private void checkStartTime() throws InvalidFFMPEGPropertiesException {
        if (properties.getStartTime() == null) {
            throw new InvalidFFMPEGPropertiesException("Start time cannot be null!");
        }
    }

    private void checkSubtitles() throws InvalidFFMPEGPropertiesException {
        if (properties.getSubtitlesPath() == null) {
            throw new InvalidFFMPEGPropertiesException("Subtitles path cannot be null!");
        }
        if (!properties.getSubtitlesPath().toFile().exists()) {
            throw new InvalidFFMPEGPropertiesException("Subtitles file does not exists!");
        }
        if (!properties.getSubtitlesPath().toFile().canRead()) {
            throw new InvalidFFMPEGPropertiesException("Cannot read subtitles file!");
        }
    }
}
