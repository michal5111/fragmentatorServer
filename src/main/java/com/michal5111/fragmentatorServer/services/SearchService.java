package com.michal5111.fragmentatorServer.services;

import com.michal5111.fragmentatorServer.domain.Line;
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
import java.util.List;

@Service
public class SearchService {

    @PersistenceContext
    private EntityManager entityManager;

    public Page<Line> search(String phrase, Pageable pageable) {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Line.class)
                .get();
        Query query = queryBuilder
                .phrase()
                .withSlop(2)
                //.simpleQueryString()
                .onField("textLines")
                .sentence(phrase)
                //.matching(phrase)
                .createQuery();
        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query, Line.class);
        jpaQuery.setFirstResult((int) pageable.getOffset());
        jpaQuery.setMaxResults(pageable.getPageSize());
        jpaQuery.setSort(Sort.RELEVANCE);
        List<Line> result = jpaQuery.getResultList();
        return new PageImpl<>(result, pageable, jpaQuery.getResultSize());
    }

    //    @GetMapping("/searchPhrase")
//    public Set<SearchPhraseResponse> searchLineIndexed(@RequestParam("phrase") String phrase) {
//        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
//        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
//                .buildQueryBuilder()
//                .forEntity(Line.class)
//                .get();
//        Query query = queryBuilder
//                .phrase()
//                .withSlop(2)
//                //.simpleQueryString()
//                .onField("textLines")
//                .sentence(phrase)
//                //.matching(phrase)
//                .createQuery();
//        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query,Line.class);
//        //jpaQuery.setMaxResults(100);
//        jpaQuery.setSort(Sort.RELEVANCE);
//        List<Line> resultList = jpaQuery.getResultList();
//        Set<SearchPhraseResponse> searchPhraseResponses = new HashSet<>();
//        resultList.forEach(line -> {
//            SearchPhraseResponse searchPhraseResponse = new SearchPhraseResponse();
//            Subtitles subtitles = line.getSubtitles();
//            Movie movie = subtitles.getMovie();
//            searchPhraseResponse.setMovie(movie);
//            searchPhraseResponses.add(movie);
//        });
//        return searchPhraseResponses;
//    }
}
