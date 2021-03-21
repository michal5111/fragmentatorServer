package com.michal5111.fragmentator_server.domain;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import java.io.File;
import java.io.Serializable;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Indexed
@EqualsAndHashCode
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(indexes = {@Index(columnList = "fileName"), @Index(columnList = "path,filename")})
public class Movie implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    @OneToOne(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Subtitles subtitles;

    @Field
    @Column(nullable = false, unique = true)
    private String fileName;

    @Field
    private String parsedTitle;

    private Integer year;

    private String resolution;

    @Field
    @Column(nullable = false)
    private String path;

    private String extension;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "movie")
    private List<FragmentRequest> fragmentRequests;

    @JsonIgnore
    @Transient
    @EqualsAndHashCode.Exclude
    private File movieFile;

    public File getMovieFile() {
        if (movieFile == null) {
            movieFile = new File(path, fileName + "." + extension);
        }
        return movieFile;
    }
}
