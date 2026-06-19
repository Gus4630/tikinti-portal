package az.tikinti.portal.model.dto.request.expense;

import az.tikinti.portal.model.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExpenseMediaRequest {

    @NotBlank
    private String url;

    @NotNull
    private MediaType mediaType;

    private String caption;

    private Integer displayOrder;
}
