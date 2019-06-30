package com.michal5111.fragmentatorServer.Entities;

public class Movie {
    private SubtitlesFile subtitles;
    private String fileName;
    private String path;
    private String extension;

    public SubtitlesFile getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(SubtitlesFile subtitles) {
        this.subtitles = subtitles;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
