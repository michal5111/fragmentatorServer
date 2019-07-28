package com.michal5111.fragmentatorServer.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Line implements Serializable {
    private int number;
    private String timeString;
    @JsonIgnore
    private LocalTime timeFrom;
    @JsonIgnore
    private LocalTime timeTo;
    private String textLines;

    public boolean parseTime() {
        String[] timeStringSplit = timeString.split(" --> ");
        if (timeStringSplit.length < 2) {
            return false;
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss,SSS");
        timeFrom = LocalTime.from(dateTimeFormatter.parse(timeStringSplit[0]));
        timeTo = LocalTime.from((dateTimeFormatter.parse(timeStringSplit[1])));
        return true;
    }
}
