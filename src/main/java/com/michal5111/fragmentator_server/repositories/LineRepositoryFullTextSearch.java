package com.michal5111.fragmentator_server.repositories;

import com.michal5111.fragmentator_server.domain.Line;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LineRepositoryFullTextSearch {
    Page<Line> fullTextSearchPhrase(String phrase, String filter, Pageable pageable);
}
