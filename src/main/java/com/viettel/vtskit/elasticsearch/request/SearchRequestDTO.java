package com.viettel.vtskit.elasticsearch.request;

import lombok.Data;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;
import java.util.Map;

@Data
public class SearchRequestDTO extends PagedRequestDTO {
    private String textSearch;
    private String sortBy;
    private SortOrder orderBy;
    private Map<String, Float> fieldsAndWeights;
    private List<String> fields;
    private int slop = 10;
    private int maxExpansions = 10;
}
