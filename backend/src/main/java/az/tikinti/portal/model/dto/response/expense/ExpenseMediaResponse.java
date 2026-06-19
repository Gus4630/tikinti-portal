package az.tikinti.portal.model.dto.response.expense;

import az.tikinti.portal.model.enums.MediaType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExpenseMediaResponse {

    private UUID id;
    private String url;
    private MediaType mediaType;
    private String caption;
    private Integer displayOrder;
}
