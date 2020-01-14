package com.michal5111.fragmentatorServer.domain;

import com.fasterxml.jackson.annotation.*;
import lombok.*;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.*;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Indexed
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(indexes = {@Index(columnList = "fileName"), @Index(columnList = "path,filename")})
public class Movie {
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
    @Column(nullable = false)
    private String path;
    private String extension;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "movie")
    private List<FragmentRequest> fragmentRequests;
}
