package com.michal5111.fragmentatorServer.domain;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;

import javax.persistence.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonDeserialize(as = SRTSubtitles.class)
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
    @Column(nullable = false)
    private String filename;
    @JsonIgnore
    @OneToMany(mappedBy = "subtitles", cascade = CascadeType.ALL)
    protected List<Line> lines = new LinkedList<>();
//    @Transient
//    @JsonInclude
//    @EqualsAndHashCode.Exclude
//    protected List<Line> filteredLines = new LinkedList<>();

    public boolean parse() throws FileNotFoundException {
        return false;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
//    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
//    @JsonIdentityReference(alwaysAsId = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Movie movie;

    public boolean saveToFile(File file) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
        lines.forEach(line -> {
            try {
                bw.write(String.valueOf(line.getNumber()));
                bw.newLine();
                bw.write(line.getTimeString());
                bw.newLine();
                bw.write(line.getTextLines().replace("<br>", "\n"));
                bw.newLine();
                bw.newLine();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return true;
    }
}
