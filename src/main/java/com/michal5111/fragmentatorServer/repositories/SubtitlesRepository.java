package com.michal5111.fragmentatorServer.repositories;

import com.michal5111.fragmentatorServer.Entities.Subtitles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubtitlesRepository extends JpaRepository<Subtitles, Long> {

}
