package com.michal5111.fragmentatorServer.parsers;

import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Subtitles;

import java.io.FileNotFoundException;
import java.util.List;

public interface SubtitlesParser {
    List<Line> parse(Subtitles subtitles) throws FileNotFoundException;
}
