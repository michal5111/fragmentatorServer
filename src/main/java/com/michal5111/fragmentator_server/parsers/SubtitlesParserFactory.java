package com.michal5111.fragmentator_server.parsers;

import com.michal5111.fragmentator_server.exceptions.UnknownSubtitlesTypeException;

public class SubtitlesParserFactory {

    public SubtitlesParser create(String subtitlesFileName) throws UnknownSubtitlesTypeException {
        if (subtitlesFileName.endsWith(".srt")) {
            return new SRTSubtitlesParser();
        }
        throw new UnknownSubtitlesTypeException();
    }
}
