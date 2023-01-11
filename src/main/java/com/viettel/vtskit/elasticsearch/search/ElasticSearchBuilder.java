package com.viettel.vtskit.elasticsearch.search;

import com.viettel.vtskit.elasticsearch.constants.PageConstants;
import com.viettel.vtskit.elasticsearch.exception.ValidateException;
import com.viettel.vtskit.elasticsearch.request.SearchRequestDTO;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ElasticSearchBuilder {

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

    private static <T> SearchSourceBuilder searchSourceBuilder(SearchRequestDTO searchRequestDTO, QueryBuilder queryBuilder, Class<T> responseDTO) {
        validateSearchRequest(searchRequestDTO);
        final int from = searchRequestDTO.getPage() <= 0 ? 0 : searchRequestDTO.getPage() * searchRequestDTO.getSize();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .fetchSource(getSource(responseDTO),null)
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

    private static <T> String [] getSource(Class<T> tClass) {
        Field[] fields = tClass.getDeclaredFields();
        String[] sources = new String[fields.length];
        int index = 0;
        for (Field field : fields) {
            sources[index] = field.getName();
            index += 1;
        }

        return sources;
    }

    private static QueryBuilder getHandleQueryBuilder(BoolQueryBuilder boolQuery) {
        return QueryBuilders.boolQuery().filter(boolQuery);
    }

    public static <T> SearchRequest buildHandleSearchRequest(String indexName, SearchRequestDTO searchRequestDTO, BoolQueryBuilder boolQuery, Class<T> responseDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getHandleQueryBuilder(boolQuery), responseDTO);
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

    public static <T> SearchRequest buildMatchPhrasePrefixSearchRequest(String indexName, SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getMatchPhrasePrefixQuery(searchRequestDTO), responseDTO);
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

    public static <T> SearchRequest buildMatchPhraseSearchRequest(String indexName, SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getMatchPhraseQuery(searchRequestDTO), responseDTO);
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

    public static <T> SearchRequest buildRegexpSearchRequest(String indexName, SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getRegexpQueryBuilder(searchRequestDTO), responseDTO);
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

    public static <T> SearchRequest buildFuzzySearchRequest(String indexName, SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getFuzzyQueriesBuilder(searchRequestDTO), responseDTO);
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

    public static <T> SearchRequest buildWildCardSearchRequest(String indexName, SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getWildCardQueryBuilder(searchRequestDTO), responseDTO);
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
        return QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch())
                .type(MultiMatchQueryBuilder.Type.MOST_FIELDS)
                .operator(Operator.OR)
                .fields(searchRequestDTO.getFieldsAndWeights());
    }

    public static <T> SearchRequest buildBoostingSearchRequest(String indexName, SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getBoostingQueryBuilder(searchRequestDTO), responseDTO);
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> SearchRequest buildMultiFieldSearchRequest(String indexName, SearchRequestDTO searchRequestDTO, Class<T> responseDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(searchRequestDTO, getMultiFieldQueryBuilder(searchRequestDTO), responseDTO);
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getMultiFieldQueryBuilder(SearchRequestDTO searchRequestDTO) {
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
