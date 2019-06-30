package com.michal5111.fragmentatorServer.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private double startOffset;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private double stopOffset;

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public LocalTime getTimeFrom() {
        return timeFrom;
    }

    public void setTimeFrom(LocalTime timeFrom) {
        this.timeFrom = timeFrom;
    }

    public LocalTime getTimeTo() {
        return timeTo;
    }

    public void setTimeTo(LocalTime timeTo) {
        this.timeTo = timeTo;
    }

    public String getTextLines() {
        return textLines;
    }

    public void setTextLines(String textLines) {
        this.textLines = textLines;
    }

    public String getTimeString() {
        return timeString;
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

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

    public double getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(double startOffset) {
        this.startOffset = startOffset;
    }

    public double getStopOffset() {
        return stopOffset;
    }

    public void setStopOffset(double stopOffset) {
        this.stopOffset = stopOffset;
    }
}
