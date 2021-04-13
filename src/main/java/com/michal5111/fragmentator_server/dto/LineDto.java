package com.michal5111.fragmentator_server.dto;

import com.michal5111.fragmentator_server.domain.Line;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Builder
@Data
@AllArgsConstructor
public class LineDto {

    private Long id;

    private int number;

    private String timeString;

    private LocalTime timeFrom;

    private LocalTime timeTo;

    private String textLines;

    public LineDto(Line line) {
        this.id = line.getId();
        this.number = line.getNumber();
        this.timeString = line.getTimeString();
        this.timeFrom = line.getTimeFrom();
        this.timeTo = line.getTimeTo();
        this.textLines = line.getTextLines();
    }
}
