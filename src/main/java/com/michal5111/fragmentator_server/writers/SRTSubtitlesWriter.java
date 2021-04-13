package com.michal5111.fragmentator_server.writers;

import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Subtitles;
import com.michal5111.fragmentator_server.dto.LineDto;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class SRTSubtitlesWriter implements SubtitlesWriter {
    @Override
    public File write(Subtitles subtitles) throws IOException {
        File file = getTempFile();
        log.info("Zapisuję napisy do pliku {}", file.getAbsolutePath());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (Line line : subtitles.getLines()) {
                writeSrtLine(bw, line);
            }
        } catch (IOException e) {
            throw new IllegalStateException("error in saving temp subtitles!", e);
        }
        return file;
    }

    @Override
    public File write(Map<Integer, LineDto> lines) throws IOException {
        File file = getTempFile();
        log.info("Zapisuję napisy do pliku {}", file.getAbsolutePath());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (LineDto line : lines.values()) {
                writeSrtLineDto(bw, line);
            }
        } catch (IOException e) {
            throw new IllegalStateException("error in saving temp subtitles!", e);
        }
        return file;
    }

    private void writeSrtLine(BufferedWriter bw, Line line) throws IOException {
        bw.write(String.valueOf(line.getNumber()));
        bw.newLine();
        bw.write(line.getTimeString());
        bw.newLine();
        bw.write(line.getTextLines().replace("<br>", "\n"));
        bw.newLine();
        bw.newLine();
    }

    private void writeSrtLineDto(BufferedWriter bw, LineDto line) throws IOException {
        bw.write(String.valueOf(line.getNumber()));
        bw.newLine();
        bw.write(line.getTimeString());
        bw.newLine();
        bw.write(line.getTextLines().replace("<br>", "\n"));
        bw.newLine();
        bw.newLine();
    }


    private File getTempFile() throws IOException {
        return File.createTempFile("temp", ".srt");
    }
}
