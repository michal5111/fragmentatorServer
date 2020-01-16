package com.michal5111.fragmentator_server.repositories;

import com.michal5111.fragmentator_server.domain.Subtitles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubtitlesRepository extends JpaRepository<Subtitles, Long> {

}
