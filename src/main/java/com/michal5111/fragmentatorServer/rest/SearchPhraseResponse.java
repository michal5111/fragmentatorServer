package com.michal5111.fragmentatorServer.rest;

import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Movie;
import lombok.Data;

import java.util.List;

@Data
public class SearchPhraseResponse {
    private Movie movie;
    private List<Line> filteredLines;
}
