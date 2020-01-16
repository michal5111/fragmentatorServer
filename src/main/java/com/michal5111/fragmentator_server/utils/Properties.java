package com.michal5111.fragmentator_server.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@Configuration
@ConfigurationProperties(prefix = "fragmentator")
@Validated
@Data
public class Properties {
    @NotEmpty
    private String videoCache;
    @NotEmpty
    private String imageCache;
    @NotEmpty
    private String conversionVideoCodec;
    @NotEmpty
    private String conversionAudioCodec;
    @NotEmpty
    private String conversionVideoFormat;
    @NotEmpty
    private String conversionImageFormat;
    @NotEmpty
    private String[] searchDirectories;
}
