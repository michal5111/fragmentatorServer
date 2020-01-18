package com.michal5111.fragmentator_server.dto;

import lombok.Data;

import java.util.List;

@Data
public class FragmentRequestDTO {

    private Long movieId;

    private Double startOffset;

    private Double stopOffset;

    private Long startLineId;

    private Long stopLineId;

    private List<LineEditDTO> lineEdits;
}
