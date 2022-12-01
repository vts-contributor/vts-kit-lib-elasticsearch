package search;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
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

    public <T> List<T> handleSearch(final String index, Object requestDTO, BoolQueryBuilder boolQuery, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildHandleSearchRequest(
                index,
                requestDTO,
                boolQuery
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> multiSearch(final String index, Object requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildMultiFieldSearchRequest(
                index,
                requestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> regexpSearch(final String index, Object requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildRegexpSearchRequest(
                index,
                requestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> fuzzySearch(final String index, Object requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildFuzzySearchRequest(
                index,
                requestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> wildCardSearch(final String index, Object requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildWildCardSearchRequest(
                index,
                requestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> matchPhraseSearch(final String index, Object requestDTO, Class<T> responseDTO) {
        SearchRequest request = ElasticSearchBuilder.buildMatchPhraseSearchRequest(
                index,
                requestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> matchPhrasePrefixSearch(final String index, Object requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildMatchPhrasePrefixSearchRequest(
                index,
                requestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> boostingSearch(final String index, Object requestDTO, Class<T> responseDTO) {
        final SearchRequest request = ElasticSearchBuilder.buildBoostingSearchRequest(
                index,
                requestDTO
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