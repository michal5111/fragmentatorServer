package com.michal5111.fragmentatorServer.Entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(indexes = {@Index(columnList = "fileName"), @Index(columnList = "path,filename")})
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;
    @OneToOne(mappedBy = "movie",cascade = CascadeType.ALL)
    private Subtitles subtitles;
    @Column(nullable = false)
    private String fileName;
    @Column(nullable = false)
    private String path;
    private String extension;
    @Transient
    @JsonInclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private double startOffset;
    @Transient
    @JsonInclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private double stopOffset;
}
