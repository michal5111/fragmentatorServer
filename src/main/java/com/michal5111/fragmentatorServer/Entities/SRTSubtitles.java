package com.michal5111.fragmentatorServer.Entities;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

@Builder
@NoArgsConstructor
@Data
public class SRTSubtitles extends Subtitles {

    public boolean parse() throws FileNotFoundException {
        Scanner scanner = new Scanner(this.subtitleFile).useDelimiter("(\n\n|\r\n\r\n)");
        while (scanner.hasNext()) {
            String scannedString = scanner.next();
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
                System.out.println(getMovie().getPath()+"/"+getFilename());
                System.out.println(Arrays.toString(splitString));
                System.out.println(e.getMessage());
            } finally {
                line.setNumber(number);
                line.setTimeString(splitString[1].trim());
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 2; i < splitString.length; i++) {
                    stringBuilder.append(splitString[i].trim());
                    if (i != splitString.length-1) {
                        stringBuilder.append("<br>");
                    }
                }
                line.setTextLines(stringBuilder.toString());
                line.setSubtitles(this);
                lines.add(line);
            }
        }
        return true;
    }
}
