package com.michal5111.fragmentatorServer.utils;

import com.michal5111.fragmentatorServer.Entities.Line;
import com.michal5111.fragmentatorServer.Entities.Movie;
import com.michal5111.fragmentatorServer.Entities.SRTSubtitlesFile;
import com.michal5111.fragmentatorServer.Entities.SubtitlesFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
                    .map(Utils::createMovieFromPath)
                    .filter(movie -> filterMovieByFraze(movie, fraze));

        }
        return Arrays.stream(streams).parallel().flatMap(Function.identity()).collect(Collectors.toList());
    }


    private static boolean endsWithSRT(Path path) {
        return path.getFileName().toString().endsWith(".srt");
    }

    private static Movie createMovieFromPath(File file) {
        Movie movie = new Movie();
        SubtitlesFile subtitlesFile = new SRTSubtitlesFile();
        subtitlesFile.setSubtitleFile(file);
        movie.setSubtitles(subtitlesFile);
        movie.getSubtitles().setFilename(file.getName());
        movie.setFileName(file.getName().substring(0,file.getName().lastIndexOf('.')));
        movie.setPath(file.getPath().substring(0,file.getPath().lastIndexOf('/')));
        return movie;
    }

    private static boolean filterMovieByFraze(Movie movie, String fraze) {
        try {
            SubtitlesFile subtitlesFile = movie.getSubtitles();
            subtitlesFile.parser();
            List<Line> lineList = subtitlesFile.getLines().stream()
                    .filter(line -> line.getTextLines().toUpperCase().contains(fraze.toUpperCase()))
                    .collect(Collectors.toList());
            if (lineList.isEmpty()) {
                return false;
            }
            subtitlesFile.setFilteredLines(lineList);
            return true;
//                    .map(Line::getTextLines)
//                    .flatMap(Arrays::stream)
//                    .anyMatch(line -> line.toUpperCase().contains(fraze.toUpperCase()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String generateSnapshotLink(Movie movie) throws IOException {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        line.parseTime();
        movie.setExtension(getMovieExtension(Paths.get(movie.getPath())));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        String timeString = dateTimeFormatter.format(line.getTimeFrom());
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                //"konsole",
               // "--hold",
                //"-e",
                "ffmpeg",
                "-y",
                "-ss", timeString,
                "-i", movie.getPath()+"/"+movie.getFileName()+movie.getExtension(),
                //"-vf","subtitles=TEMP.srt",
                "-frames:v", "1",
               "/home/michal/Obrazy/SpringFragmenterCache/"+movie.getFileName()+line.getNumber()+".jpg"
        );
        Process process = processBuilder.start();
        return movie.getFileName().replace(" ","%20")+line.getNumber();
    }

    private static String getMovieExtension(Path path) throws IOException {
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

    public static String generateFragmentLink(Movie movie) throws IOException {
        Line line = movie.getSubtitles().getFilteredLines().get(0);
        line.parseTime();
        movie.setExtension(getMovieExtension(Paths.get(movie.getPath())));
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        LocalTime time = line.getTimeFrom().minusMinutes(1).plusNanos((new Double(line.getStartOffset()*1000000000.0)).longValue());
        double to =  line.getTimeTo().getHour()*3600+line.getTimeTo().getMinute()*60+line.getTimeTo().getSecond()+line.getTimeTo().getNano()/1000000000.0+line.getStopOffset()
                - (line.getTimeFrom().getHour()*3600+line.getTimeFrom().getMinute()*60+line.getTimeFrom().getSecond()+line.getTimeFrom().getNano()/1000000000.0+line.getStartOffset());
        String timeString = dateTimeFormatter.format(time);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(
                //"konsole",
                 //"--hold",
                //"-e",
                "ffmpeg",
                "-y",
                "-ss", timeString,
                "-i", movie.getPath()+"/"+movie.getFileName()+movie.getExtension(),
                "-ss", "00:01:00",
                "-t", String.format(Locale.US, "%.3f", to),
                "-acodec", "copy",
                "-vcodec", "h264",
                "-preset", "veryslow",
                //"-vf", "subtitles=TEMP.srt",
                "/home/michal/Wideo/SpringFragmenterCache/"+movie.getFileName()+line.getNumber()+".mp4"
        );
        Process process = processBuilder.start();
        return movie.getFileName()+line.getNumber();
    }
}