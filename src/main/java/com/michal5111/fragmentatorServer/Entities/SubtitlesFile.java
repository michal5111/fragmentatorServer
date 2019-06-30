package com.michal5111.fragmentatorServer.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(as = SRTSubtitlesFile.class)
public abstract class SubtitlesFile implements Serializable {
    @JsonIgnore
    protected File subtitleFile;
    private String filename;
    @JsonIgnore
    protected List<Line> lines = new LinkedList<>();
    protected List<Line> filteredLines = new LinkedList<>();
    public abstract boolean parser() throws FileNotFoundException;

    public File getSubtitleFile() {
        return subtitleFile;
    }

    public void setSubtitleFile(File subtitleFile) {
        this.subtitleFile = subtitleFile;
    }

    public List<Line> getLines() {
        return lines;
    }

    public List<Line> getFilteredLines() {
        return filteredLines;
    }

    public void setFilteredLines(List<Line> filteredLines) {
        this.filteredLines = filteredLines;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public abstract void prepareForConversion();

}
