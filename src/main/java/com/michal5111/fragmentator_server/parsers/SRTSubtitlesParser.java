package com.michal5111.fragmentator_server.parsers;

import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Subtitles;
import org.apache.logging.log4j.util.Strings;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class SRTSubtitlesParser implements SubtitlesParser {

    public List<Line> parse(Subtitles subtitles) throws ParseException {
        File subtitleFile = subtitles.getSubtitleFile();
        List<Line> lines = new LinkedList<>();
        int lineNumber = 0;
        try (Scanner scanner = new Scanner(subtitleFile).useDelimiter("(\n\n|\r\n\r\n)")) {
            while (scanner.hasNext()) {
                lineNumber++;
                String scannedString = scanner.next();
                String[] splitString = splitLines(scannedString);
                splitString = fixLineOffset(splitString);
                if (splitString.length < 3) {
                    continue;
                }
                Line line = new Line();
                int number = Integer.parseInt(splitString[0].trim().strip());

                String textLines = buildTextString(splitString);

                line.setNumber(number);
                line.setTimeString(splitString[1].trim());
                line.setTextLines(textLines);
                line.setSubtitles(subtitles);
                line.parseTime();
                lines.add(line);
            }
        } catch (NumberFormatException e) {
            throw new ParseException(
                    String.format(
                            "Unable to parse line number! %s/%s %s %s line number: %d",
                            subtitles.getMovie().getPath(),
                            subtitles.getFilename(),
                            e.getClass().getSimpleName(),
                            e.getMessage(),
                            lineNumber
                    )
                    , lineNumber
            );
        } catch (IllegalArgumentException e) {
            throw new ParseException(
                    String.format(
                            "Unable to parse time: %s/%s %s %s  line number: %d",
                            subtitles.getMovie().getPath(),
                            subtitles.getFilename(),
                            e.getClass().getSimpleName(),
                            e.getMessage(),
                            lineNumber
                    )
                    , lineNumber
            );
        } catch (FileNotFoundException e) {
            throw new ParseException("File not found or is unreadable", 0);
        } catch (Exception e) {
            throw new ParseException(
                    String.format(
                            "%s/%s %s %s line number: %d",
                            subtitles.getMovie().getPath(),
                            subtitles.getFilename(),
                            e.getClass().getSimpleName(),
                            e.getMessage(),
                            lineNumber
                    ),
                    lineNumber
            );
        }
        return lines;
    }

    private String[] splitLines(String string) {
        String[] splitString = string.split("\n");
        if (splitString.length > 0) {
            splitString[0] = splitString[0]
                    .replaceAll("\uFEFF", "");
        }
        return splitString;
    }

    private String buildTextString(String[] strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 2; i < strings.length; i++) {
            stringBuilder.append(strings[i].trim());
            if (i != strings.length - 1) {
                stringBuilder.append("<br>");
            }
        }
        return stringBuilder.toString();
    }

    private String[] fixLineOffset(String[] strings) {
        int offset = 0;
        int size = strings.length;
        while (offset < size && Strings.isBlank(strings[offset])) {
            offset++;
        }
        return Arrays.copyOfRange(strings, offset, size);
    }
}
