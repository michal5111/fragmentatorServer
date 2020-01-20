package com.michal5111.fragmentator_server.parsers;

import com.michal5111.fragmentator_server.exceptions.UnknownSubtitlesTypeException;

public class SubtitlesParserFactory {

    private final String subtitlesFileName;

    public SubtitlesParserFactory(String subtitlesFileName) {
        this.subtitlesFileName = subtitlesFileName;
    }

    public SubtitlesParser create() throws UnknownSubtitlesTypeException {
        if (subtitlesFileName.endsWith(".srt")) {
            return new SRTSubtitlesParser();
        }
        throw new UnknownSubtitlesTypeException();
    }
}
