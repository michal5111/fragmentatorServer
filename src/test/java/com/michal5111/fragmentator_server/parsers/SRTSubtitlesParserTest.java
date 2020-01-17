package com.michal5111.fragmentator_server.parsers;

import com.michal5111.fragmentator_server.domain.Line;
import com.michal5111.fragmentator_server.domain.Movie;
import com.michal5111.fragmentator_server.domain.Subtitles;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SRTSubtitlesParserTest {

    @Test
    void givenValidSRTSubtitles_thenReturnListOfLines() throws ParseException {
        Movie movie = new Movie();
        movie.setPath("src/test/resources/srtFiles");
        Subtitles subtitles = new Subtitles();
        subtitles.setMovie(movie);
        subtitles.setFilename("valid.srt");
        SRTSubtitlesParser srtSubtitlesParser = new SRTSubtitlesParser();
        List<Line> lineList = srtSubtitlesParser.parse(subtitles);
        assertEquals(3, lineList.size());
        assertEquals(1, lineList.get(0).getNumber());
        assertEquals("00:00:51,759 --> 00:00:56,639", lineList.get(0).getTimeString());
        assertEquals("NIEDALEKA PRZYSZŁOŚĆ.", lineList.get(0).getTextLines());
        assertEquals(2, lineList.get(1).getNumber());
        assertEquals("00:00:56,680 --> 00:01:02,520", lineList.get(1).getTimeString());
        assertEquals("CZAS PEŁEN NADZIEI ORAZ SPORÓW.", lineList.get(1).getTextLines());
        assertEquals(3, lineList.get(2).getNumber());
        assertEquals("00:01:02,561 --> 00:01:09,569", lineList.get(2).getTimeString());
        assertEquals("LUDZKOŚĆ PATRZY KU GWIAZDOM, WYPATRUJĄC<br>ROZUMNYCH ISTOT ORAZ ŚCIEŻEK ROZWOJU.", lineList.get(2).getTextLines());
    }

    @Test
    void givenInvalidSRTSubtitlesWithDoubleNewLine_thenReturnListOfLines() throws ParseException {
        Movie movie = new Movie();
        movie.setPath("src/test/resources/srtFiles");
        Subtitles subtitles = new Subtitles();
        subtitles.setMovie(movie);
        subtitles.setFilename("invalidDoubleNewLine.srt");
        SRTSubtitlesParser srtSubtitlesParser = new SRTSubtitlesParser();
        List<Line> lineList = srtSubtitlesParser.parse(subtitles);
        assertEquals(3, lineList.size());
        assertEquals(1, lineList.get(0).getNumber());
        assertEquals("00:00:51,759 --> 00:00:56,639", lineList.get(0).getTimeString());
        assertEquals("NIEDALEKA PRZYSZŁOŚĆ.", lineList.get(0).getTextLines());
        assertEquals(2, lineList.get(1).getNumber());
        assertEquals("00:00:56,680 --> 00:01:02,520", lineList.get(1).getTimeString());
        assertEquals("CZAS PEŁEN NADZIEI ORAZ SPORÓW.", lineList.get(1).getTextLines());
        assertEquals(3, lineList.get(2).getNumber());
        assertEquals("00:01:02,561 --> 00:01:09,569", lineList.get(2).getTimeString());
        assertEquals("LUDZKOŚĆ PATRZY KU GWIAZDOM, WYPATRUJĄC<br>ROZUMNYCH ISTOT ORAZ ŚCIEŻEK ROZWOJU.", lineList.get(2).getTextLines());
    }

    @Test
    void givenInvalidSRTSubtitlesEmptyText_thenReturnListOfLines() throws ParseException {
        Movie movie = new Movie();
        movie.setPath("src/test/resources/srtFiles");
        Subtitles subtitles = new Subtitles();
        subtitles.setMovie(movie);
        subtitles.setFilename("invalidEmptyText.srt");
        SRTSubtitlesParser srtSubtitlesParser = new SRTSubtitlesParser();
        List<Line> lineList = srtSubtitlesParser.parse(subtitles);
        assertEquals(3, lineList.size());
        assertEquals(302, lineList.get(0).getNumber());
        assertEquals("00:21:37,631 --> 00:21:40,467", lineList.get(0).getTimeString());
        assertEquals("Może odrobinę.", lineList.get(0).getTextLines());
        assertEquals(304, lineList.get(1).getNumber());
        assertEquals("00:21:49,393 --> 00:21:54,606", lineList.get(1).getTimeString());
        assertEquals("Napisy pobrane z www.simpsons.go.pl", lineList.get(1).getTextLines());
        assertEquals(305, lineList.get(2).getNumber());
        assertEquals("00:21:55,606 --> 00:21:59,556", lineList.get(2).getTimeString());
        assertEquals("www.NapiProjekt.pl - nowa jakość napisów.<br>Napisy zostały specjalnie dopasowane do Twojej wersji filmu.", lineList.get(2).getTextLines());
    }

    @Test
    void givenInvalidSRTSubtitlesEmptyDoubleText_thenReturnListOfLines() throws ParseException {
        Movie movie = new Movie();
        movie.setPath("src/test/resources/srtFiles");
        Subtitles subtitles = new Subtitles();
        subtitles.setMovie(movie);
        subtitles.setFilename("invalidDoubleEmptyLine.srt");
        SRTSubtitlesParser srtSubtitlesParser = new SRTSubtitlesParser();
        List<Line> lineList = srtSubtitlesParser.parse(subtitles);
        assertEquals(2, lineList.size());
        assertEquals(7, lineList.get(0).getNumber());
        assertEquals("00:00:43,000 --> 00:00:46,533", lineList.get(0).getTimeString());
        assertEquals("Nie ma nic zwariowanego w tym.<br>to tylko Freedom Day!", lineList.get(0).getTextLines());
        assertEquals(10, lineList.get(1).getNumber());
        assertEquals("00:00:55,000 --> 00:00:59,000", lineList.get(1).getTimeString());
        assertEquals("Co to takiego, ten Freedom Day?<br>Brzmi jak jakiś tani produkt.", lineList.get(1).getTextLines());
    }
}