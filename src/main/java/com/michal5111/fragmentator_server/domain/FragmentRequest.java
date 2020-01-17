package com.michal5111.fragmentator_server.domain;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.michal5111.fragmentator_server.deserializers.LineIdDeserializer;
import com.michal5111.fragmentator_server.deserializers.MovieIdDeserializer;
import com.michal5111.fragmentator_server.enums.FragmentRequestStatus;
import com.michal5111.fragmentator_server.utils.TempFileStore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

@Entity
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", referencedColumnName = "id")
    private Movie movie;

    private Double startOffset;

    private Double stopOffset;

    @Enumerated(EnumType.STRING)
    private FragmentRequestStatus status = FragmentRequestStatus.PENDING;

    private String errorMessage;

    @JsonProperty("startLineId")
    @JsonDeserialize(using = LineIdDeserializer.class)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "start_line_id", referencedColumnName = "id")
    private Line startLine;

    @JsonProperty("stopLineId")
    @JsonDeserialize(using = LineIdDeserializer.class)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_line_id", referencedColumnName = "id")
    private Line stopLine;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "fragmentRequest", orphanRemoval = true)
    private List<LineEdit> lineEdits;

    private String resultFileName;

    @Transient
    @JsonIgnore
    private TempFileStore tempFiles = new TempFileStore();

    @Transient
    @JsonIgnore
    private List<Line> lines = new LinkedList<>();
}