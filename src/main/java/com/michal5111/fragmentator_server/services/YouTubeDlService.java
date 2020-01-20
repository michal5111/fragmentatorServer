package com.michal5111.fragmentator_server.services;

import com.michal5111.fragmentator_server.enums.DownloadStatusType;
import com.michal5111.fragmentator_server.enums.YouTubeDlProcessType;
import com.michal5111.fragmentator_server.exceptions.YouTubeDlPropertiesException;
import com.michal5111.fragmentator_server.utils.Properties;
import com.michal5111.fragmentator_server.youtube.YouTubeDlProperties;
import com.michal5111.fragmentator_server.youtube.YouTubeDlWrapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class YouTubeDlService {

    private final Properties properties;
    private final Path outputPath = Paths.get("/var/cache/fragmenter/SpringFragmenterCache/youtube");

    public YouTubeDlService(Properties properties) {
        this.properties = properties;
    }

    public Flux<String> getInfo(String url) {
        YouTubeDlProperties youTubeDlProperties = YouTubeDlProperties.builder()
                .youTubeDlProcessType(YouTubeDlProcessType.INFO)
                .url(url)
                .build();
        YouTubeDlWrapper youTubeDlWrapper = new YouTubeDlWrapper(youTubeDlProperties);
        try {
            youTubeDlWrapper.prepare();
        } catch (YouTubeDlPropertiesException e) {
            return Flux.error(e);
        }
        return youTubeDlWrapper.getInputFlux();
    }

    public Flux<DownloadStatus> download(String url) {
        YouTubeDlProperties youTubeDlProperties = YouTubeDlProperties.builder()
                .youTubeDlProcessType(YouTubeDlProcessType.DOWNLOAD)
                .url(url)
                .outputDirectory(outputPath)
                .audioCodec(properties.getConversionAudioCodec())
                .build();
        YouTubeDlWrapper youTubeDlWrapper = new YouTubeDlWrapper(youTubeDlProperties);
        try {
            youTubeDlWrapper.prepare();
        } catch (YouTubeDlPropertiesException e) {
            return Flux.error(e);
        }
        return youTubeDlWrapper.getInputFlux()
                .map(this::stringToDownloadStatus);
    }

    private DownloadStatus stringToDownloadStatus(String s) {
        String typeString = s.substring(0, s.indexOf(' '));
        String messageString = s.substring(s.indexOf(' ') + 1);
        String percentageString = "100";
        if (messageString.contains("%")) {
            percentageString = messageString.substring(0, messageString.lastIndexOf('%'));
        }
        Float percentage = Float.valueOf(percentageString);
        if (DownloadStatusType.DOWNLOAD.value.equals(typeString)) {
            return DownloadStatus.builder()
                    .downloadStatusType(DownloadStatusType.DOWNLOAD)
                    .message(messageString)
                    .percentage(percentage)
                    .build();
        } else if (DownloadStatusType.INFO.value.equals(typeString)) {
            return DownloadStatus.builder()
                    .downloadStatusType(DownloadStatusType.INFO)
                    .message(messageString)
                    .build();
        } else if (DownloadStatusType.WARNING.value.equals(typeString)) {
            return DownloadStatus.builder()
                    .downloadStatusType(DownloadStatusType.WARNING)
                    .message(messageString)
                    .build();
        } else if (DownloadStatusType.FFMPEG.value.equals(typeString)) {
            return DownloadStatus.builder()
                    .downloadStatusType(DownloadStatusType.FFMPEG)
                    .message(messageString)
                    .build();
        } else if (DownloadStatusType.YOUTUBE.value.equals(typeString)) {
            return DownloadStatus.builder()
                    .downloadStatusType(DownloadStatusType.YOUTUBE)
                    .message(messageString)
                    .build();
        }
        return DownloadStatus.builder()
                .downloadStatusType(DownloadStatusType.OTHER)
                .message(String.format("TYPE: %s MESSAGE: %s", typeString, messageString))
                .build();
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class DownloadStatus {
        private DownloadStatusType downloadStatusType;
        private String message;
        private Float percentage;
    }
}
