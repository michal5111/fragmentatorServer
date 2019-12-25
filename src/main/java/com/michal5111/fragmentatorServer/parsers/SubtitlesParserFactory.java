package com.michal5111.fragmentatorServer.parsers;

import com.michal5111.fragmentatorServer.exceptions.UnknownSubtitlesTypeException;

public class SubtitlesParserFactory {

    private String subtitlesFileName;

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
