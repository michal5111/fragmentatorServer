package com.michal5111.fragmentatorServer.Entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Movie {
    private Subtitles subtitles;
    private String fileName;
    private String path;
    private String extension;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private double startOffset;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private double stopOffset;
}
