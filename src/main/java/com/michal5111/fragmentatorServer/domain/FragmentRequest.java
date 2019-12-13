package com.michal5111.fragmentatorServer.domain;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.michal5111.fragmentatorServer.deserializers.LineIdDeserializer;
import com.michal5111.fragmentatorServer.deserializers.MovieIdDeserializer;
import com.michal5111.fragmentatorServer.enums.FragmentRequestStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode
@ToString
public class FragmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("movieId")
    @JsonDeserialize(using = MovieIdDeserializer.class)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", referencedColumnName = "id")
    private Movie movie;

    private Double startOffset;

    private Double stopOffset;

    @Enumerated(EnumType.STRING)
    private FragmentRequestStatus status;

    private String errorMessage;

    @JsonProperty("startLineId")
    @JsonDeserialize(using = LineIdDeserializer.class)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_line_id", referencedColumnName = "id")
    private Line startLine;

    @JsonProperty("stopLineId")
    @JsonDeserialize(using = LineIdDeserializer.class)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stop_line_id", referencedColumnName = "id")
    private Line stopLine;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "fragmentRequest")
    private List<LineEdit> lineEdits;

    private String resultFileName;
}
