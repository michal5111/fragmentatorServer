package com.michal5111.fragmentatorServer.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Movie {
    private SubtitlesFile subtitles;
    private String fileName;
    private String path;
    private String extension;
}
