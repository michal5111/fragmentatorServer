package com.michal5111.fragmentatorServer.parsers;

import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Subtitles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class SRTSubtitlesParser implements SubtitlesParser {

    private final Logger logger = LoggerFactory.getLogger(SRTSubtitlesParser.class);

    public List<Line> parse(Subtitles subtitles) throws FileNotFoundException {
        File subtitleFile = subtitles.getSubtitleFile();
        List<Line> lines = new LinkedList<>();
        if (subtitleFile != null) {
            if (!subtitleFile.exists() || !subtitleFile.canRead()) {
                throw new FileNotFoundException();
            }
        } else return lines;
        Scanner scanner = new Scanner(subtitleFile).useDelimiter("(\n\n|\r\n\r\n)");
        logger.debug("Parsing " + subtitleFile.getAbsolutePath());
        while (scanner.hasNext()) {
            String scannedString = scanner.next();
            logger.debug(scannedString);
            String[] splitString = scannedString.split("\n");
            if (splitString.length < 3) {
                continue;
            }
            splitString[0] = splitString[0].replaceAll("(ď»ż|\uFEFF|˙ţ|\n)?", "");
            Line line = new Line();
            int number = 0;
            try {
                if (!splitString[0].trim().equals(""))
                    number = Integer.parseInt(splitString[0].trim().strip());
            } catch (NumberFormatException e) {
                logger.warn(subtitles.getMovie().getPath() + "/" + subtitles.getFilename());
                logger.warn(Arrays.toString(splitString));
                logger.warn(e.getMessage());
            } finally {
                line.setNumber(number);
                line.setTimeString(splitString[1].trim());
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 2; i < splitString.length; i++) {
                    stringBuilder.append(splitString[i].trim());
                    if (i != splitString.length - 1) {
                        stringBuilder.append("<br>");
                    }
                }
                line.setTextLines(stringBuilder.toString());
                line.setSubtitles(subtitles);
                try {
                    line.parseTime();
                } catch (IllegalArgumentException e) {
                    logger.warn(subtitles.getMovie().getPath() + "/" + subtitles.getFilename());
                    logger.warn(String.valueOf(line.getNumber()));
                    logger.warn(line.getTimeString());
                    logger.warn(e.getMessage());
                }
                lines.add(line);
            }
        }
        scanner.close();
        return lines;
    }
}
