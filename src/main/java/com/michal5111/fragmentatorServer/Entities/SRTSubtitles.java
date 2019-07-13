package com.michal5111.fragmentatorServer.Entities;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.FileNotFoundException;
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
            Line line = new Line();
            String numberString = splitString[0];
            if (numberString.endsWith("\r")) {
                numberString = numberString.substring(0,numberString.lastIndexOf("\r"));
            }
            int number = 0;
            try {
                number = Integer.parseInt(numberString.trim().strip());
            } catch (NumberFormatException ignored) {
            }
            line.setNumber(number);
            line.setTimeString(splitString[1].split("\r")[0].trim());
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 2; i < splitString.length; i++) {

                stringBuilder.append(splitString[i].split("\r")[0].trim());
                if (i != scannedString.length()-1) {
                    stringBuilder.append("<br>");
                }
            }
            line.setTextLines(stringBuilder.toString());
            lines.add(line);
        }
        return true;
    }

    @Override
    public void prepareForConversion() {
        lines.forEach(Line::parseTime);
    }


}
