package com.michal5111.fragmentator_server.parsers;

import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Subtitles;

import java.text.ParseException;
import java.util.List;

public interface SubtitlesParser {
    List<Line> parse(Subtitles subtitles) throws ParseException;
}
