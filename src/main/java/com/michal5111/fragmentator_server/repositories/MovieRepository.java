package com.michal5111.fragmentator_server.repositories;

import com.michal5111.fragmentator_server.domain.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Query(value = "select m from Movie m where lower(m.fileName) like lower(concat('%',:title,'%')) or lower(m.parsedTitle) like lower(concat('%',:title,'%'))")
    List<Movie> findMovieByFileNameContainingIgnoreCase(String title);

    @Query(value = "select * from movie where lower(file_name) like lower(concat('%',:title,'%')) or lower(parsed_title) like lower(concat('%',:title,'%')) limit 20", nativeQuery = true)
    List<Movie> findTitleHints(@Param("title") String title);

    Boolean existsByFileNameEquals(String filename);

}
