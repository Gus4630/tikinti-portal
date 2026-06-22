package az.tikinti.portal.model.dto.request.group;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvitationRequest {

    @NotNull
    private UUID userId;

    private String memberRole;
}
