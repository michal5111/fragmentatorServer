package com.michal5111.fragmentator_server.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "fragmentator")
@Validated
@Data
public class Properties {
    private String videoCache;

    private String imageCache;

    private String conversionVideoCodec;

    private String conversionAudioCodec;

    private String conversionVideoFormat;

    private String conversionImageFormat;

    private String[] searchDirectories;
}
