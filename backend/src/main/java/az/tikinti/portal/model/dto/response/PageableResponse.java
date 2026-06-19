package az.tikinti.portal.model.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageableResponse<T> {

    @Builder.Default
    private List<T> content = new ArrayList<>();

    private int page;
    private int size;
    private boolean last;
    private int totalPages;
    private long totalElements;
}
