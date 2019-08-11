package com.michal5111.fragmentatorServer.repositories;

import com.michal5111.fragmentatorServer.Entities.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Query(value = "select * from movie m left join subtitles s on s.movie_id = m.id where s.id in  (select subtitles_id from line where lower(text_lines) like lower(concat('%',:phrase,'%')))",nativeQuery = true)
    List<Movie> findMoviesByPhrase(@Param("phrase") String phrase);

    @Query(value = "select m from Movie m inner join m.subtitles s where s in (select l.subtitles from Line l where lower(text_lines) like lower(concat('%',:phrase,'%')))")
    List<Movie> findMoviesByPhrase2(@Param("phrase") String phrase);

    List<Movie> findMovieByFileNameContainingIgnoreCase(String title);

    @Query(value = "select * from movie where lower(file_name) like lower(concat('%',:title,'%')) limit 20",nativeQuery = true)
    List<Movie> findTitleHints(@Param("title") String title);

    Optional<Movie> findByPathAndFileNameEquals(String path,String filename);

}
