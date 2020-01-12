package com.michal5111.fragmentatorServer.repositories;

import com.michal5111.fragmentatorServer.domain.Line;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineRepository extends CrudRepository<Line, Long> {
    List<Line> findAllByTextLinesContainingIgnoreCase(String phrase);

    @Query(value = "select text_lines from line where lower(text_lines) like lower(concat('%',:phrase,'%')) limit 20", nativeQuery = true)
    List<String> findText(@Param("phrase") String phrase);

    @Query(value = "select * from line where lower(text_lines) like lower(concat('%',:phrase,'%')) limit 20", nativeQuery = true)
    List<Line> findText2(@Param("phrase") String phrase);

    @Query(value = "select * from line l where l.subtitles_id = :p_subtitles_id and lower(l.text_lines) like lower(concat('%',:p_phrase,'%'));", nativeQuery = true)
    List<Line> findFilteredLines(@Param("p_subtitles_id") Long movieId, @Param("p_phrase") String phrase);

    List<Line> findAllBySubtitles_Movie_IdAndTextLinesContainingIgnoreCase(Long movieId, String phrase);

    List<Line> findAllBySubtitles_Movie_Id(@Param("p_movie_id") Long movieId);

    List<Line> findAllByIdBetween(Long startId, Long stopId);
}
