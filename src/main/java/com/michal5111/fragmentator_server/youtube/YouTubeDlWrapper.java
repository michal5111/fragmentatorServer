package com.michal5111.fragmentator_server.youtube;

import com.michal5111.fragmentator_server.enums.YouTubeDlProcessType;
import com.michal5111.fragmentator_server.exceptions.YouTubeDlException;
import com.michal5111.fragmentator_server.exceptions.YouTubeDlPropertiesException;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class YouTubeDlWrapper {

    private final YouTubeDlProperties youTubeDlProperties;

    private ProcessBuilder processBuilder;

    public YouTubeDlWrapper(YouTubeDlProperties youTubeDlProperties) {
        this.youTubeDlProperties = youTubeDlProperties;
    }

    private ProcessBuilder prepareDownloadProcess() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "youtube-dl",
                "--newline",
                "--sub-lang", "pl,en",
                "--write-auto-sub",
                "-k",
                "--convert-subs", "srt",
                "--audio-format", youTubeDlProperties.getAudioCodec(),
                youTubeDlProperties.getUrl()
        );
        pb.directory(youTubeDlProperties.getOutputDirectory().toFile());
        return pb;
    }

    private ProcessBuilder prepareInfoProcess() {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "youtube-dl",
                "--dump-single-json",
                youTubeDlProperties.getUrl()
        );
        return pb;
    }

    public void prepare() throws YouTubeDlPropertiesException {
        checkProperties();
        switch (youTubeDlProperties.getYouTubeDlProcessType()) {
            case INFO:
                processBuilder = prepareInfoProcess();
                break;
            case DOWNLOAD:
                processBuilder = prepareDownloadProcess();
                break;
            default:
                throw new YouTubeDlPropertiesException("Unknown youtube-dl process type");
        }
    }

    private void checkProperties() throws YouTubeDlPropertiesException {
        if (youTubeDlProperties.getYouTubeDlProcessType() == YouTubeDlProcessType.DOWNLOAD) {
            checkOutputDirectory();
        }
    }

    private void checkOutputDirectory() throws YouTubeDlPropertiesException {
        Path path = youTubeDlProperties.getOutputDirectory();
        if (!Files.isDirectory(path)) {
            throw new YouTubeDlPropertiesException("Output is not a directory");
        }
        if (!Files.isWritable(path)) {
            throw new YouTubeDlPropertiesException("Directory is not writeable");
        }
    }

    public Flux<String> getInputFlux() {
        processBuilder.redirectErrorStream(true);
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            return Flux.error(e);
        }
        final BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );
        return Flux.fromStream(reader.lines())
                .handle((s, sink) -> {
                    if (s.contains("error:")) {
                        String error = s.split("error:")[1];
                        sink.error(new YouTubeDlException(error));
                    }
                    sink.next(s);
                });
    }
}
