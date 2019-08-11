package com.michal5111.fragmentatorServer.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

@ConfigurationProperties(prefix="fragmentator")
@Validated
@Data
public class Properties {
    @NotEmpty
    private String videoCache;
    @NotEmpty
    private String imageCache;
    @NotEmpty
    private String conversionVideoFormat;
    @NotEmpty
    private String conversionAudioFormat;
}
