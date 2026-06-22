package az.tikinti.portal.dao.entity.group;

import az.tikinti.portal.dao.entity.BaseEntity;
import az.tikinti.portal.dao.entity.auth.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Table(name = "group_invitations")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GroupInvitationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_user_id", nullable = false)
    private UserEntity invitedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private UserEntity invitedBy;

    @Column(name = "member_role")
    private String memberRole;

    @Column(name = "status", nullable = false)
    private String status;
}
