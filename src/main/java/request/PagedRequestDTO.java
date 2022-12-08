package request;

import lombok.Data;

@Data
public class PagedRequestDTO {
    private int page = 0;
    private int size = 50;
}
