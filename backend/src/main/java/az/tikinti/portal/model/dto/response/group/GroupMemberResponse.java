package az.tikinti.portal.model.dto.response.group;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {

    private UUID memberId;
    private UUID userId;
    private String username;
    private String fullName;
    private String email;
    private String memberRole;
}
