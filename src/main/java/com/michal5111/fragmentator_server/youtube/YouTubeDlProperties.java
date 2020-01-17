package com.michal5111.fragmentator_server.youtube;

import com.michal5111.fragmentator_server.enums.YouTubeDlProcessType;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder
public class YouTubeDlProperties {

    private String url;

    private Path outputDirectory;

    private String startTime;

    private Double timeLength;

    private YouTubeDlProcessType youTubeDlProcessType;

    private String videoCodec;

    private String audioCodec;
}
