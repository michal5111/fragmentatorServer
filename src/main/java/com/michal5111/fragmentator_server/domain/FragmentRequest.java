package com.michal5111.fragmentator_server.domain;

import com.fasterxml.jackson.annotation.*;
import com.michal5111.fragmentator_server.enums.FragmentRequestStatus;
import com.michal5111.fragmentator_server.utils.TempFileStore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@Entity
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EqualsAndHashCode
@ToString
public class FragmentRequest implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @JsonProperty("movieId")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", referencedColumnName = "id")
    private Movie movie;

    private Double startOffset;

    private Double stopOffset;

    @Enumerated(EnumType.STRING)
    @EqualsAndHashCode.Exclude
    private FragmentRequestStatus status = FragmentRequestStatus.PENDING;

    @EqualsAndHashCode.Exclude
    private String errorMessage;

    @JsonProperty("startLineId")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "start_line_id", referencedColumnName = "id")
    private Line startLine;

    @JsonProperty("stopLineId")
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

    @EqualsAndHashCode.Exclude
    private String resultFileName;

    @Transient
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private transient TempFileStore tempFiles = new TempFileStore();

    @Transient
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private transient List<Line> lines = new LinkedList<>();
}
