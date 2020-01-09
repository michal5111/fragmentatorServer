package com.michal5111.fragmentatorServer.utils;

import com.michal5111.fragmentatorServer.domain.Line;
import com.michal5111.fragmentatorServer.domain.Movie;
import com.michal5111.fragmentatorServer.domain.Subtitles;
import com.michal5111.fragmentatorServer.exceptions.MovieNotFoundException;
import reactor.core.publisher.Flux;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Stream;

public class Utils {

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
        try {
            movie.setExtension(getMovieExtension(Paths.get(movie.getPath()), subtitles.getFilename()));
        } catch (IOException | MovieNotFoundException e) {
            e.printStackTrace();
        }
        subtitles.setMovie(movie);
        return movie;
    }

    public static String getMovieExtension(Path path, String srtFileName) throws IOException, MovieNotFoundException {
        Optional<Path> video = Files.walk(path)
                .filter(Files::isRegularFile)
                .filter(x -> x.getFileName().toString().contains(srtFileName))
                .filter(x -> {
                    try {
                        if (Files.probeContentType(x) == null) {
                            return false;
                        }
                        return Files.probeContentType(x).contains("video") ||
                                x.getFileName().toString().endsWith(".divx") ||
                                x.getFileName().toString().endsWith(".rmvb");
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }).findAny();
        if (video.isPresent()) {
            String pathString = video.get().toString();
            return pathString.substring(pathString.lastIndexOf("."));
        }
        throw new MovieNotFoundException("Movie not found");
    }

    public static Flux<Movie> getMovieExtension2(Movie movie) throws IOException, MovieNotFoundException {
        Path path = Paths.get(movie.getPath()).toAbsolutePath();
        Stream<Movie> stream = Files.walk(path)
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .filter(x -> x.toString().contains(movie.getSubtitles().getFilename()))
                .filter(x -> {
                    try {
                        if (Files.probeContentType(x) == null) {
                            return false;
                        }
                        return Files.probeContentType(x).contains("video") ||
                                x.endsWith(".divx") ||
                                x.endsWith(".rmvb");
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .map(Path::toString)
                .filter(path1 -> path1.contains("."))
                .map(path1 -> path1.substring(path1.lastIndexOf(".") + 1))
                .map(s -> {
                    movie.setExtension(s);
                    return movie;
                });
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
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp));
        bufferedWriter.write("1\n");
        bufferedWriter.write("00:00:00.000 --> 10:00:00.000\n");
        bufferedWriter.write(line.getTextLines() + "\n");
        bufferedWriter.close();
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