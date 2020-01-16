package com.michal5111.fragmentator_server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;

import javax.persistence.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Subtitles implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;
    @JsonIgnore
    @Transient
    protected File subtitleFile;
    @JsonIgnore
    @OneToMany(mappedBy = "subtitles", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Line> lines = new LinkedList<>();
    @Field
    @Column(nullable = false, unique = true)
    private String filename;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
//    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
//    @JsonIdentityReference(alwaysAsId = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @IndexedEmbedded
    private Movie movie;

    public void saveToFile(File file) {
        lines.forEach(line -> {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
                bw.write(String.valueOf(line.getNumber()));
                bw.newLine();
                bw.write(line.getTimeString());
                bw.newLine();
                bw.write(line.getTextLines().replace("<br>", "\n"));
                bw.newLine();
                bw.newLine();

            } catch (IOException e) {
                throw new IllegalStateException("error in saving temp subtitles!", e);
            }
        });
    }

    public File getSubtitleFile() {
        if (subtitleFile == null) {
            subtitleFile = new File(getMovie().getPath(), getFilename());
        }
        return subtitleFile;
    }
}
