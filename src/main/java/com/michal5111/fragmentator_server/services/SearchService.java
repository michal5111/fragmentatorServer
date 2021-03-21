package com.michal5111.fragmentator_server.services;

import com.michal5111.fragmentator_server.domain.Line;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.LinkedList;
import java.util.List;

@Service
public class SearchService {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Line> search(String phrase, String filter, Pageable pageable) {
        try {
            FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
            QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
                    .buildQueryBuilder()
                    .forEntity(Line.class)
                    .get();

            Query textQuery = queryBuilder
                    .phrase()
                    .withSlop(2)
                    .onField("textLines")
                    .sentence(phrase)
                    .createQuery();

            Query query = textQuery;

            if (filter != null && !"".equals(filter)) {
                filter = "*" + filter.toLowerCase().replace(" ", "?") + "*";
                Query titleQuery = queryBuilder
                        .keyword()
                        .wildcard()
                        .onField("subtitles.movie.fileName")
                        .matching(filter)
                        .createQuery();

                query = queryBuilder.bool()
                        .must(textQuery)
                        .must(titleQuery)
                        .createQuery();
            }
            FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query, Line.class);
            jpaQuery.setFirstResult((int) pageable.getOffset());
            jpaQuery.setMaxResults(pageable.getPageSize());
            jpaQuery.setSort(Sort.RELEVANCE);
            List<Line> result = jpaQuery.getResultList();
            return new PageImpl<>(result, pageable, jpaQuery.getResultSize());
        } catch (Exception e) {
            return new PageImpl<>(new LinkedList<>(), pageable, 0);
        }
    }
}
