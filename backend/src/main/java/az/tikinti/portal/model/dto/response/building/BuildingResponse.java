package az.tikinti.portal.model.dto.response.building;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
public class BuildingResponse {

    private UUID id;
    private String name;
    private String address;
    private String description;
    private BigDecimal floorAreaM2;
    private BigDecimal budgetLimit;
    private UUID groupId;
    private String groupName;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal totalSpent;
    private List<BuildingMediaResponse> media;
}
