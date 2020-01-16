package com.michal5111.fragmentator_server.ffmpeg;

import com.michal5111.fragmentator_server.enums.FFMPEGConversionType;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder
public class FFMPEGProperties {

    private Path inputFilePath;

    private String startTime;

    private Double timeLength;

    private Path subtitlesPath;

    private String videoCodec;

    private String audioCodec;

    private Path outputFilePath;

    private FFMPEGConversionType conversionType;
}
