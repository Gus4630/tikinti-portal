package az.tikinti.portal.model.dto.request.building;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingRequest {

    @NotBlank
    private String name;

    private String address;

    private String description;

    @Positive
    private BigDecimal floorAreaM2;

    @Positive
    private BigDecimal budgetLimit;

    private java.util.UUID groupId;

    private List<BuildingMediaRequest> media;
}
