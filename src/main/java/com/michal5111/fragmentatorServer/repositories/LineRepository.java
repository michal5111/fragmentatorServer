package com.michal5111.fragmentatorServer.repositories;

import com.michal5111.fragmentatorServer.Entities.Line;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineRepository extends CrudRepository<Line, Long> {
    List<Line> findAllByTextLinesContainingIgnoreCase(String fraze);

    @Query(value = "select text_lines from line where lower(text_lines) like lower(concat('%',:fraze,'%')) limit 20",nativeQuery = true)
    List<String> findText(@Param("fraze") String fraze);

    @Query(value = "select * from line where lower(text_lines) like lower(concat('%',:fraze,'%')) limit 20",nativeQuery = true)
    List<Line> findText2(@Param("fraze") String fraze);

    @Query(value = "select * from line l where l.subtitles_id = :p_subtitles_id and lower(l.text_lines) like lower(concat('%',:p_fraze,'%'));", nativeQuery = true)
    List<Line> findFilteredLines(@Param("p_subtitles_id") Long movieId, @Param("p_fraze") String fraze);

    List<Line> findAllBySubtitles_Movie_IdAndTextLinesContainingIgnoreCase(Long movieId, String fraze);

    List<Line> findAllBySubtitles_Movie_Id(@Param("p_movie_id") Long movieId);
}
