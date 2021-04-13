package com.michal5111.fragmentator_server.utils;

import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Movie;
import com.michal5111.fragmentator_server.domain.Subtitles;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import reactor.core.publisher.Flux;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Utils {

    private static final Pattern movieTitlePattern = Pattern.compile("^(.*?)\\W(?:(\\d{4})(?:\\W(\\d+p)?)|(\\d+p)(?:\\W(\\d{4}))?)\\b");

    private Utils() {
    }

    public static boolean endsWithSRT(Path path) {
        return path.getFileName().toString().endsWith(".srt");
    }

    public static Movie createMovieFromFile(File file) {
        Subtitles subtitles = new Subtitles();
        subtitles.setSubtitleFile(file);
        subtitles.setFilename(file.getName());
        Matcher m = movieTitlePattern.matcher(file.getName());
        String title = null;
        Integer year = null;
        String resolution = null;
        if (m.find()) {
            title = m.group(1).replace(".", " ");
            if (m.group(2) != null) {
                try {
                    year = Integer.parseInt(m.group(2));
                } catch (Exception ignored) {
                }
                resolution = m.group(3);
            } else {
                resolution = m.group(4);
                if (m.group(5) != null) {
                    try {
                        year = Integer.parseInt(m.group(5));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        Movie movie = Movie.builder()
                .subtitles(subtitles)
                .fileName(file.getName().substring(0, file.getName().lastIndexOf('.')))
                .parsedTitle(title)
                .year(year)
                .resolution(resolution)
                .path(file.getParent())
                .build();
        subtitles.setMovie(movie);
        return movie;
    }

    public static Flux<Path> fileFlux(Path path) {
        return Flux.create(sink -> {
            try {
                Files.walkFileTree(
                        path,
                        new HashSet<>(Collections.singletonList(FileVisitOption.FOLLOW_LINKS)),
                        Integer.MAX_VALUE,
                        new SimpleFileVisitor<>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                log.debug("Visiting file {}", file.getFileName());
                                sink.next(file);
                                return FileVisitResult.CONTINUE;
                            }
                        }
                );
                sink.complete();
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }

    public static Flux<Movie> getMovieExtension(Movie movie) {
        Path path = Paths.get(movie.getPath()).toAbsolutePath();
        String subtitlesFilename = movie.getSubtitles().getFilename();
        Tika tika = new Tika();
        return fileFlux(path)
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .filter(x -> x.toString().contains(subtitlesFilename.substring(0, subtitlesFilename.lastIndexOf('.'))))
                .filter(fileName -> {
                    log.debug("Content type: {}", tika.detect(movie.getPath() + fileName));
                    return tika.detect(movie.getPath() + fileName).contains("video");
                })
                .map(Path::toString)
                .filter(path1 -> path1.contains("."))
                .map(path1 -> path1.substring(path1.lastIndexOf('.') + 1))
                .map(s -> {
                    movie.setExtension(s);
                    return movie;
                });
    }

    public static double timeToSeconds(String time) {
        String[] split = time.split(":");
        double hours = Double.parseDouble(split[0]);
        double minutes = Double.parseDouble(split[1]);
        double seconds = Double.parseDouble(split[2]);
        return hours * 3600 + minutes * 60 + seconds;
    }

    public static File createTempSubtitles(Line line) throws IOException {
        File temp = File.createTempFile("temp", ".srt");
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp))) {
            bufferedWriter.write("1\n");
            bufferedWriter.write("00:00:00.000 --> 10:00:00.000\n");
            bufferedWriter.write(line.getTextLines() + "\n");
        }
        return temp;
    }

    public static double calculateDuration(LocalTime timeFrom, LocalTime timeTo, double startOffset, double stopOffset) {
        return timeTo.getHour() * 3600 + timeTo.getMinute() * 60
                + timeTo.getSecond()
                + timeTo.getNano() / 1000000000.0
                + stopOffset
                - (timeFrom.getHour() * 3600
                + timeFrom.getMinute() * 60
                + timeFrom.getSecond()
                + timeFrom.getNano() / 1000000000.0
                + startOffset);
    }
}