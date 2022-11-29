package request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.search.sort.SortOrder;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequestDTO extends PagedRequestDTO {
    private String textSearch;
    private String sortBy;
    private SortOrder orderBy;
    private Map<String, Float> fieldsAndWeight;
    private List<String> fields;
    private int slop;
    private int maxExpansions;
}
