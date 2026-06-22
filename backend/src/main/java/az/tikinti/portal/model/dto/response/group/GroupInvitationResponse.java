package az.tikinti.portal.model.dto.response.group;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvitationResponse {

    private UUID id;
    private UUID groupId;
    private String groupName;
    private UUID invitedUserId;
    private String invitedUserUsername;
    private String invitedUserFullName;
    private UUID invitedById;
    private String invitedByUsername;
    private String invitedByFullName;
    private String memberRole;
    private String status;
    private LocalDateTime createdAt;
}
