package com.michal5111.fragmentatorServer.utils;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.Entities.SRTSubtitles;
import com.michal5111.fragmentatorServer.Entities.Subtitles;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    static public List<Movie> findFraze(String fraze) throws IOException {
        Path[] paths = new Path[]{
                Paths.get("/disks/G/Pobrane/Filmy/"),
                Paths.get("/disks/G/Pobrane/Seriale/"),
                Paths.get("/disks/E/Downloads/Filmy/"),
                Paths.get("/disks/E/Downloads/Seriale/"),
                Paths.get("/disks/G/kopia/Downloads/Seriale/")
        };
        Stream<Movie>[] streams = new Stream[paths.length];
        for (int i = 0; i < paths.length-1; i++) {
            streams[i] = Files.walk(paths[i])
                    .filter(Files::isRegularFile)
                    .filter(Utils::endsWithSRT)
                    .map(Path::toFile)
                    .map(Utils::createMovieFromFile)
                    .filter(movie -> filterMovieByFraze(movie, fraze));

        }
        return Arrays.stream(streams)
                .parallel()
                .flatMap(Function.identity())
                .sorted(Comparator.comparing(Movie::getFileName))
                .collect(Collectors.toList());
    }


    private static boolean endsWithSRT(Path path) {
        return path.getFileName().toString().endsWith(".srt");
    }

    private static Movie createMovieFromFile(File file) {
        Subtitles subtitles = new SRTSubtitles();
        subtitles.setSubtitleFile(file);
        subtitles.setFilename(file.getName());
        return  Movie.builder()
                .subtitles(subtitles)
                .fileName(file.getName().substring(0,file.getName().lastIndexOf('.')))
                .path(file.getParent())
                .build();
    }

    private static boolean filterMovieByFraze(Movie movie, String fraze) {
        try {
            Subtitles subtitles = movie.getSubtitles();
            subtitles.parse();
            List<Line> lineList = subtitles.getLines().stream()
                    .filter(line -> line.getTextLines().toUpperCase().contains(fraze.toUpperCase()))
                    .collect(Collectors.toList());
            if (lineList.isEmpty()) {
                return false;
            }
            subtitles.setFilteredLines(lineList);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getMovieExtension(Path path) throws IOException {
        Optional<Path> video = Files.walk(path)
                .filter(Files::isRegularFile)
                .filter(x -> {
                    try {
                        return Files.probeContentType(x).contains("video");
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }).findAny();
        if (video.isPresent()) {
            String pathString = video.get().toString();
            return pathString.substring(pathString.lastIndexOf("."));
        }
        return null;
    }

    public static double timeToSeconds(String time) {
        String[] split = time.split(":");
        double hours = Double.valueOf(split[0]);
        double minutes = Double.valueOf(split[1]);
        double seconds = Double.valueOf(split[2]);
        return hours*3600+minutes*60+seconds;
    }

    public static File createTempSubtitles(Movie movie) throws IOException {
        File temp = File.createTempFile("temp",".srt");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp));
        bufferedWriter.write("1\n");
        bufferedWriter.write("00:00:00.000 --> 10:00:00.000\n");
        bufferedWriter.write(movie.getSubtitles().getFilteredLines().get(0).getTextLines()+"\n");
        bufferedWriter.close();
        return temp;
    }
}