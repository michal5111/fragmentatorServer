package com.michal5111.fragmentatorServer.rest;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import lombok.Data;

import java.util.List;

@Data
public class SearchPhraseResponse {
    private Movie movie;
    private List<Line> filteredLines;
}
