package com.viettel.vtskit.elasticsearch.search;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viettel.vtskit.elasticsearch.request.SearchRequestDTO;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElasticsearchService {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);

    @Autowired
    private RestHighLevelClient client;

    public <T> List<T> handleSearch(final String index, SearchRequestDTO requestDTO, BoolQueryBuilder boolQuery, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildHandleSearchRequest(
                index,
                requestDTO,
                boolQuery,
                responseDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> multiSearch(final String index, SearchRequestDTO requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildMultiFieldSearchRequest(
                index,
                requestDTO,
                responseDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> regexpSearch(final String index, SearchRequestDTO requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildRegexpSearchRequest(
                index,
                requestDTO,
                responseDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> fuzzySearch(final String index, SearchRequestDTO requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildFuzzySearchRequest(
                index,
                requestDTO,
                responseDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> wildCardSearch(final String index, SearchRequestDTO requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildWildCardSearchRequest(
                index,
                requestDTO,
                responseDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> matchPhraseSearch(final String index, SearchRequestDTO requestDTO, Class<T> responseDTO) {
        SearchRequest request = ElasticSearchBuilder.buildMatchPhraseSearchRequest(
                index,
                requestDTO,
                responseDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> matchPhrasePrefixSearch(final String index, SearchRequestDTO requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildMatchPhrasePrefixSearchRequest(
                index,
                requestDTO,
                responseDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> boostingSearch(final String index, SearchRequestDTO requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildBoostingSearchRequest(
                index,
                requestDTO,
                responseDTO
        );

        return searchInternal(request, responseDTO);
    }
    public <T> List<T> searchInternal(final SearchRequest request, Class<T> responseDTO) {
        if (request == null) {
            LOG.error("Failed to build search request");
            return Collections.emptyList();
        }

        try {
            final SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            final SearchHit[] searchHits = response.getHits().getHits();
            List<T> list = new ArrayList<>(searchHits.length);
            for (SearchHit hit : searchHits) {
                list.add(
                        MAPPER.readValue(hit.getSourceAsString(), responseDTO)
                );
            }

            return list;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}