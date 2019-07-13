package com.michal5111.fragmentatorServer.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonDeserialize(as = SRTSubtitles.class)
public abstract class Subtitles implements Serializable {
    @JsonIgnore
    protected File subtitleFile;
    private String filename;
    @JsonIgnore
    protected List<Line> lines = new LinkedList<>();
    protected List<Line> filteredLines = new LinkedList<>();

    public abstract boolean parse() throws FileNotFoundException;

    public abstract void prepareForConversion();

}
