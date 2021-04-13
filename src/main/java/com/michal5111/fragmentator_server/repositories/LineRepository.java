package com.michal5111.fragmentator_server.repositories;

import com.michal5111.fragmentator_server.domain.Line;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineRepository extends CrudRepository<Line, Long>, LineRepositoryFullTextSearch {

    @Query(value = "select * from line where lower(text_lines) like lower(concat('%',:phrase,'%')) limit 20", nativeQuery = true)
    List<Line> findText2(@Param("phrase") String phrase);

    List<Line> findAllBySubtitlesMovieIdOrderByNumber(@Param("p_movie_id") Long movieId);

    List<Line> findAllByIdBetween(Long startId, Long stopId);
}
