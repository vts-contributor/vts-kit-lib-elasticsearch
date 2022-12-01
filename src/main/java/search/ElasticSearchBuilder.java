package search;

import com.fasterxml.jackson.core.JsonProcessingException;
import constants.PageConstants;
import exception.ValidateException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.util.CollectionUtils;
import request.SearchRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.ArrayList;
import java.util.List;

public class ElasticSearchBuilder {

    private static boolean isSortBy(SearchRequestDTO searchRequestDTO) {
        return (searchRequestDTO.getSortBy() != null);
    }

    private static SearchRequestDTO convertObjectToClass(Object o) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = ow.writeValueAsString(o);
        SearchRequestDTO searchRequestDTO = new ObjectMapper().readValue(json, SearchRequestDTO.class);

        return searchRequestDTO;
    }

    private static void validateSearchRequest(SearchRequestDTO searchRequestDTO) {
        if (searchRequestDTO.getSize() > 1000) {
            throw new ValidateException(PageConstants.PAGE_SIZE_MUST_BE_LESS_THAN_OR_BY_1000);
        }

        if (searchRequestDTO.getTextSearch().isEmpty() || searchRequestDTO.getTextSearch().trim().isEmpty()) {
            throw new ValidateException(PageConstants.TEXT_SEARCH_NOT_MUST_BE_NULL_OR_CONTAINS_SPACES);
        }
    }

    private static SearchSourceBuilder searchSourceBuilder(Object requestDTO, QueryBuilder queryBuilder) throws JsonProcessingException {
        SearchRequestDTO searchRequestDTO = convertObjectToClass(requestDTO);
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

    public static SearchRequest buildHandleSearchRequest(String indexName, Object requestDTO, BoolQueryBuilder boolQuery) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(requestDTO, getHandleQueryBuilder(boolQuery));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getMatchPhrasePrefixQuery(Object requestDTO) throws JsonProcessingException {
        SearchRequestDTO searchRequestDTO = convertObjectToClass(requestDTO);

        return searchRequestDTO.getFields().stream()
                .findFirst().map(field -> QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch(), field)
                        .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX)
                        .slop(searchRequestDTO.getSlop())
                        .maxExpansions(searchRequestDTO.getMaxExpansions()))
                .orElse(null);
    }

    public static SearchRequest buildMatchPhrasePrefixSearchRequest(String indexName, Object requestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(requestDTO, getMatchPhrasePrefixQuery(requestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getMatchPhraseQuery(Object requestDTO) throws JsonProcessingException {
        SearchRequestDTO searchRequestDTO = convertObjectToClass(requestDTO);

        MultiMatchQueryBuilder queryBuilder =  QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch())
                .type(MultiMatchQueryBuilder.Type.PHRASE)
                .operator(Operator.OR)
                .slop(searchRequestDTO.getSlop());
        searchRequestDTO.getFields().forEach(queryBuilder::field);

        return queryBuilder;
    }

    public static SearchRequest buildMatchPhraseSearchRequest(String indexName, Object requestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(requestDTO, getMatchPhraseQuery(requestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getRegexpQueryBuilder(Object requestDTO) throws JsonProcessingException {
        SearchRequestDTO searchRequestDTO = convertObjectToClass(requestDTO);
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();
        searchRequestDTO.getFields().forEach(
                field -> boolQuery.should(QueryBuilders.regexpQuery(field, searchRequestDTO.getTextSearch()).caseInsensitive(true)));

        return QueryBuilders.boolQuery().filter(boolQuery);
    }

    public static SearchRequest buildRegexpSearchRequest(String indexName, Object requestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(requestDTO, getRegexpQueryBuilder(requestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getFuzzyQueriesBuilder(Object requestDTO) throws JsonProcessingException {
        SearchRequestDTO searchRequestDTO = convertObjectToClass(requestDTO);
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

    public static SearchRequest buildFuzzySearchRequest(String indexName, Object requestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(requestDTO, getFuzzyQueriesBuilder(requestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getWildCardQueryBuilder(Object requestDTO) throws JsonProcessingException {
        SearchRequestDTO searchRequestDTO = convertObjectToClass(requestDTO);
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

    public static SearchRequest buildWildCardSearchRequest(String indexName, Object requestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(requestDTO, getWildCardQueryBuilder(requestDTO));
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

    private static QueryBuilder getBoostingQueryBuilder(Object requestDTO) throws JsonProcessingException {
        SearchRequestDTO searchRequestDTO = convertObjectToClass(requestDTO);

        return QueryBuilders.multiMatchQuery(searchRequestDTO.getTextSearch())
                .type(MultiMatchQueryBuilder.Type.MOST_FIELDS)
                .operator(Operator.OR)
                .fields(searchRequestDTO.getFieldsAndWeight());
    }

    public static SearchRequest buildBoostingSearchRequest(String indexName, Object requestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(requestDTO, getBoostingQueryBuilder(requestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SearchRequest buildMultiFieldSearchRequest(String indexName, Object requestDTO) {
        try {
            SearchSourceBuilder searchSourceBuilder = searchSourceBuilder(requestDTO, getMultiFieldQueryBuilder(requestDTO));
            SearchRequest searchRequest = new SearchRequest(indexName);
            searchRequest.source(searchSourceBuilder);

            return searchRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static QueryBuilder getMultiFieldQueryBuilder(Object requestDTO) throws JsonProcessingException {
        SearchRequestDTO searchRequestDTO = convertObjectToClass(requestDTO);

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
