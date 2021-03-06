package com.michal5111.fragmentator_server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.TermVector;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Indexed
@EqualsAndHashCode
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(indexes = {@Index(columnList = "textLines"),@Index(columnList = "subtitles_id")})
public class Line implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    private int number;

    private String timeString;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @Column(columnDefinition = "TIME(3)")
    private LocalTime timeFrom;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @Column(columnDefinition = "TIME(3)")
    private LocalTime timeTo;

    @Field(termVector = TermVector.YES)
    @Lob
    @Column(length = 4096, nullable = false)
    private String textLines;

    public void parseTime() {
        String[] timeStringSplit = timeString.split(" --> ");
        if (timeStringSplit.length < 2) {
            throw new IllegalArgumentException("Time string is invalid");
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss,SSS");
        timeFrom = LocalTime.from(dateTimeFormatter.parse(timeStringSplit[0]));
        timeTo = LocalTime.from((dateTimeFormatter.parse(timeStringSplit[1])));
    }

//    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
//    @JsonIdentityReference(alwaysAsId = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtitles_id", nullable = false, referencedColumnName = "id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @IndexedEmbedded(depth = 2)
    private Subtitles subtitles;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "line")
    private LineEdit lineEdit;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "startLine")
    private List<FragmentRequest> startLineFragmentRequests;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "stopLine")
    private List<FragmentRequest> stopLineFragmentRequests;
}
