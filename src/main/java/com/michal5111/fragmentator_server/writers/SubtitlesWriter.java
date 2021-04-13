package com.michal5111.fragmentator_server.writers;

import com.michal5111.fragmentator_server.domain.Subtitles;
import com.michal5111.fragmentator_server.dto.LineDto;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface SubtitlesWriter {
    File write(Subtitles subtitles) throws IOException;

    File write(Map<Integer, LineDto> lines) throws IOException;
}
