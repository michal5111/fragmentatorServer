package com.michal5111.fragmentatorServer.ffmpeg;

import com.michal5111.fragmentatorServer.enums.FFMPEGConversionType;
import com.michal5111.fragmentatorServer.exceptions.InvalidFFMPEGPropertiesException;
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

    private ProcessBuilder prepareVideoProcess() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-y",
                "-ss", properties.getStartTime(),
                "-i", properties.getInputFilePath().toString(),
                "-ss", "00:00:01",
                "-t", String.format(Locale.US, "%.3f", properties.getTimeLength()),
                "-acodec", properties.getAudioCodec(),
                "-vcodec", properties.getVideoCodec(),
                "-preset", "veryslow",
                "-vf", "subtitles=" + properties.getSubtitlesPath().toString(),
                "-hide_banner",
                properties.getOutputFilePath().toString()
        );
        return processBuilder;
    }

    private ProcessBuilder prepareSubtitlesProcess() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-hide_banner",
                "-y",
                "-i", properties.getInputFilePath().toString(),
                "-ss", properties.getStartTime(),
                properties.getOutputFilePath().toString()
        );
        return processBuilder;
    }

    private ProcessBuilder prepareImageProcess() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                "ffmpeg",
                "-hide_banner",
                "-n",
                "-ss", properties.getStartTime(),
                "-i", properties.getInputFilePath().toString(),
                "-vf", "subtitles=" + properties.getSubtitlesPath(),
                "-frames:v", "1",
                properties.getOutputFilePath().toString()
        );
        return processBuilder;
    }

    private void checkProperties() throws InvalidFFMPEGPropertiesException {
        if (properties == null) {
            throw new InvalidFFMPEGPropertiesException("Properties cannot be null!");
        }
        if (properties.getConversionType() == null) {
            throw new InvalidFFMPEGPropertiesException("Conversion type cannot be null!");
        }
        if (properties.getInputFilePath() == null) {
            throw new InvalidFFMPEGPropertiesException("Input path cannot be null!");
        }
        if (!properties.getInputFilePath().toFile().exists()) {
            throw new InvalidFFMPEGPropertiesException("Input file does not exists!");
        }
        if (!properties.getInputFilePath().toFile().canRead()) {
            throw new InvalidFFMPEGPropertiesException("Cannot read input file!");
        }
        if (properties.getOutputFilePath() == null) {
            throw new InvalidFFMPEGPropertiesException("Output path cannot be null!");
        }
//        if (!properties.getOutputFilePath().toFile().canWrite()) {
//            throw new InvalidFFMPEGPropertiesException("Cannot write output file!");
//        }
        if (properties.getStartTime() == null) {
            throw new InvalidFFMPEGPropertiesException("Start time cannot be null!");
        }
        if (properties.getConversionType() == FFMPEGConversionType.VIDEO
                || properties.getConversionType() == FFMPEGConversionType.IMAGE) {
            if (properties.getSubtitlesPath() == null) {
                throw new InvalidFFMPEGPropertiesException("Subtitles path cannot be null!");
            }
            if (!properties.getSubtitlesPath().toFile().exists()) {
                throw new InvalidFFMPEGPropertiesException("Subtitles file does not exists!");
            }
            if (!properties.getSubtitlesPath().toFile().canRead()) {
                throw new InvalidFFMPEGPropertiesException("Cannot read subtitles file!");
            }
            if (properties.getConversionType() == FFMPEGConversionType.VIDEO) {
                if (properties.getTimeLength() == null || properties.getTimeLength() <= 0) {
                    throw new InvalidFFMPEGPropertiesException("Invalid time length!");
                }
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
                    if (s.contains("Conversion failed!")) {
                        throw new IllegalStateException("Conversion failed!");
                    }
                });
    }
}
