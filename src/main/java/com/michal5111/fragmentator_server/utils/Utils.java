package com.michal5111.fragmentator_server.utils;

import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Movie;
import com.michal5111.fragmentator_server.domain.Subtitles;
import reactor.core.publisher.Flux;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.stream.Stream;

public class Utils {

    private Utils() {
    }

    public static boolean endsWithSRT(Path path) {
        return path.getFileName().toString().endsWith(".srt");
    }

    public static Movie createMovieFromFile(File file) {
        Subtitles subtitles = new Subtitles();
        subtitles.setSubtitleFile(file);
        subtitles.setFilename(file.getName());
        Movie movie = Movie.builder()
                .subtitles(subtitles)
                .fileName(file.getName().substring(0, file.getName().lastIndexOf('.')))
                .path(file.getParent())
                .build();
        subtitles.setMovie(movie);
        return movie;
    }

    public static Flux<Movie> getMovieExtension(Movie movie) {
        Path path = Paths.get(movie.getPath()).toAbsolutePath();
        String subtitlesFilename = movie.getSubtitles().getFilename();
        Stream<Movie> stream;
        try {
            stream = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .filter(x -> x.toString().contains(subtitlesFilename.substring(0, subtitlesFilename.lastIndexOf('.'))))
                    .filter(fileName -> {
                        try {
                            if (Files.probeContentType(fileName) == null) {
                                return false;
                            }
                            return Files.probeContentType(fileName).contains("video") ||
                                    fileName.endsWith("divx") ||
                                    fileName.endsWith("rmvb");
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(Path::toString)
                    .filter(path1 -> path1.contains("."))
                    .map(path1 -> path1.substring(path1.lastIndexOf('.') + 1))
                    .map(s -> {
                        movie.setExtension(s);
                        return movie;
                    });
        } catch (IOException e) {
            return Flux.error(e);
        }
        return Flux.fromStream(stream);
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