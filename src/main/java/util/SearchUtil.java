package util;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import constants.PageConstants;
import exception.ValidateException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import request.SearchRequestDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SearchUtil.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    private final RestHighLevelClient client;

    private SearchUtil(RestHighLevelClient client) {
        this.client = client;
    }

    public <T> List<T> handleSearch(final String index, final SearchRequestDTO searchRequestDTO, BoolQueryBuilder boolQuery, Class<T> responseDTO) {
        final SearchRequest request = SearchUtil.buildHandleSearchRequest(
                index,
                searchRequestDTO,
                boolQuery
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> multiSearch(final String index, final SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        final SearchRequest request = SearchUtil.buildMultiFieldSearchRequest(
                index,
                searchRequestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> regexpSearch(final String index, final SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        final SearchRequest request = SearchUtil.buildRegexpSearchRequest(
                index,
                searchRequestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> fuzzySearch(final String index, final SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        final SearchRequest request = SearchUtil.buildFuzzySearchRequest(
                index,
                searchRequestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> wildCardSearch(final String index, final SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        final SearchRequest request = SearchUtil.buildWildCardSearchRequest(
                index,
                searchRequestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> matchPhraseSearch(final String index, final SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        SearchRequest request = SearchUtil.buildMatchPhraseSearchRequest(
                index,
                searchRequestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> matchPhrasePrefixSearch(final String index, final SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        final SearchRequest request = SearchUtil.buildMatchPhrasePrefixSearchRequest(
                index,
                searchRequestDTO
        );

        return searchInternal(request, responseDTO);
    }

    public <T> List<T> boostingSearch(final String index, final SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        final SearchRequest request = SearchUtil.buildBoostingSearchRequest(
                index,
                searchRequestDTO
        );

        return searchInternal(request, responseDTO);
    }

    private <T> List<T> searchInternal(final SearchRequest request, Class<T> responseDTO) {
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

    private static boolean isSortBy(SearchRequestDTO searchRequestDTO) {
        return (searchRequestDTO.getSortBy() != null);
    }

    private static void validateSearchRequest(SearchRequestDTO searchRequestDTO) {
        if (searchRequestDTO.getSize() > 1000) {
            throw new ValidateException(PageConstants.PAGE_SIZE_MUST_BE_LESS_THAN_OR_BY_1000);
        }

        if (searchRequestDTO.getTextSearch().isEmpty() || searchRequestDTO.getTextSearch().trim().isEmpty()) {
            throw new ValidateException(PageConstants.TEXT_SEARCH_NOT_MUST_BE_NULL_OR_CONTAINS_SPACES);
        }
    }

    private static SearchSourceBuilder searchSourceBuilder(SearchRequestDTO searchRequestDTO, QueryBuilder queryBuilder) {
        validateSearchRequest(searchRequestDTO);
        final int from = searchRequestDTO.getPage() <= 0 ? 0 : searchRequestDTO.getPage() * searchRequestDTO.getSize();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .from(from)
                .size(searchRequestDTO.getSize())
                .query(queryBuilder);

        if (isSortBy(searchRequestDTO)) {
            searchSourceBuilder = searchSourceBuilder.sort(
                    searchRequestDTO.getSortBy(),
                    searchRequestDTO.getOrderBy() != null ? searchRequestDTO.getOrderBy() : SortOrder.ASC
            );
        }

        return searchSourceBuilder;
    }

    private static QueryBuilder getHandleQueryBuilder(BoolQueryBuilder boolQuery) {
        return QueryBuilders.boolQuery().filter(boolQuery);
    }

    private static SearchRequest buildHandleSearchRequest(String indexName, SearchRequestDTO searchRequestDTO, BoolQueryBuilder boolQuery) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getHandleQueryBuilder(boolQuery));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getMatchPhrasePrefixQuery(SearchRequestDTO searchRequestDTO) {
        return searchRequestDTO.getFields().stream()
                .findFirst().map(field -> QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch(), field)
                        .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX)
                        .slop(searchRequestDTO.getSlop())
                        .maxExpansions(searchRequestDTO.getMaxExpansions()))
                        .orElse(null);
    }

    private static SearchRequest buildMatchPhrasePrefixSearchRequest(String indexName, SearchRequestDTO searchRequestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getMatchPhrasePrefixQuery(searchRequestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getMatchPhraseQuery(SearchRequestDTO searchRequestDTO) {
        MultiMatchQueryBuilder queryBuilder =  QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch())
                .type(MultiMatchQueryBuilder.Type.PHRASE)
                .operator(Operator.OR)
                .slop(searchRequestDTO.getSlop());
        searchRequestDTO.getFields().forEach(queryBuilder::field);

        return queryBuilder;
    }

    private static SearchRequest buildMatchPhraseSearchRequest(String indexName, SearchRequestDTO searchRequestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getMatchPhraseQuery(searchRequestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getRegexpQueryBuilder(SearchRequestDTO searchRequestDTO) {
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        searchRequestDTO.getFields().forEach(
                field -> boolQuery.should(QueryBuilders.regexpQuery(field, searchRequestDTO.getTextSearch()).caseInsensitive(true)));

        return QueryBuilders.boolQuery().filter(boolQuery);
    }

    private static SearchRequest buildRegexpSearchRequest(String indexName, SearchRequestDTO searchRequestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getRegexpQueryBuilder(searchRequestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getFuzzyQueriesBuilder(SearchRequestDTO searchRequestDTO) {
        if (searchRequestDTO == null){
            return null;
        }

        final List<String> fields = searchRequestDTO.getFields();
        MultiMatchQueryBuilder queryBuilder =  QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch())
                .type(MultiMatchQueryBuilder.Type.MOST_FIELDS)
                .operator(Operator.OR)
                .fuzziness("AUTO");

        fields.forEach(queryBuilder::field);

        return queryBuilder;
    }

    private static SearchRequest buildFuzzySearchRequest(String indexName, SearchRequestDTO searchRequestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getFuzzyQueriesBuilder(searchRequestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getWildCardQueryBuilder(SearchRequestDTO searchRequestDTO) {
        if (CollectionUtils.isEmpty(searchRequestDTO.getFields())) {
            return null;
        }

        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        List<WildcardQueryBuilder> wildcards = new ArrayList<>();
        searchRequestDTO.getFields().forEach(field -> {
            WildcardQueryBuilder wildcard = new WildcardQueryBuilder(field, searchRequestDTO.getTextSearch());
            wildcards.add(wildcard.caseInsensitive(true));
        });
        wildcards.forEach(boolQuery::should);

        return QueryBuilders.boolQuery().filter(boolQuery);
    }

    private static SearchRequest buildWildCardSearchRequest(String indexName, SearchRequestDTO searchRequestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getWildCardQueryBuilder(searchRequestDTO));
            if (searchSourceBuilder == null) {
                return null;
            }
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getBoostingQueryBuilder(SearchRequestDTO searchRequestDTO) {
        if (searchRequestDTO == null){
            return null;
        }

        return QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch())
                .type(MultiMatchQueryBuilder.Type.MOST_FIELDS)
                .operator(Operator.OR)
                .fields(searchRequestDTO.getFieldsAndWeight());
    }

    private static SearchRequest buildBoostingSearchRequest(String indexName, SearchRequestDTO searchRequestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getBoostingQueryBuilder(searchRequestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static SearchRequest buildMultiFieldSearchRequest(String indexName, SearchRequestDTO searchRequestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getMultiFieldQueryBuilder(searchRequestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getMultiFieldQueryBuilder(SearchRequestDTO searchRequestDTO) {
        if (searchRequestDTO == null){
            return null;
        }

        final List<String> fields = searchRequestDTO.getFields();
        if (CollectionUtils.isEmpty(fields)) {
            return QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch(), "*")
                    .type(MultiMatchQueryBuilder.Type.MOST_FIELDS);
        }

        if (fields.size() > 1) {
            MultiMatchQueryBuilder queryBuilder = QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch())
                    .type(MultiMatchQueryBuilder.Type.MOST_FIELDS)
                    .operator(Operator.OR);

            fields.forEach(queryBuilder::field);

            return queryBuilder;
        }

        return fields.stream()
                .findFirst()
                .map(field ->
                        QueryBuilders.matchQuery(field, searchRequestDTO.getTextSearch())
                                .operator(Operator.OR))
                .orElse(null);
    }
}