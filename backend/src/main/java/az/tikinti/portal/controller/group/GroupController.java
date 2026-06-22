package az.tikinti.portal.controller.group;

import static org.springframework.http.HttpStatus.CREATED;

import az.tikinti.portal.model.dto.request.group.GroupInvitationRequest;
import az.tikinti.portal.model.dto.request.group.GroupRequest;
import az.tikinti.portal.model.dto.response.group.GroupInvitationResponse;
import az.tikinti.portal.model.dto.response.group.GroupResponse;
import az.tikinti.portal.service.group.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    public ResponseEntity<List<GroupResponse>> findAll(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(groupService.findAll(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.getById(id));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(
            Authentication authentication,
            @Valid @RequestBody GroupRequest request) {
        UUID creatorId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(CREATED).body(groupService.create(request, creatorId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> update(
            @PathVariable UUID id, @Valid @RequestBody GroupRequest request) {
        return ResponseEntity.ok(groupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/invitations")
    public ResponseEntity<List<GroupInvitationResponse>> getGroupInvitations(
            @PathVariable UUID id, Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(groupService.getGroupInvitations(id, requesterId));
    }

    @PostMapping("/{id}/invitations")
    public ResponseEntity<GroupInvitationResponse> sendInvitation(
            @PathVariable UUID id,
            Authentication authentication,
            @Valid @RequestBody GroupInvitationRequest request) {
        UUID invitedById = UUID.fromString(authentication.getName());
        return ResponseEntity.status(CREATED).body(groupService.sendInvitation(id, request, invitedById));
    }

    @DeleteMapping("/{id}/invitations/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable UUID id, @PathVariable UUID invitationId,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        groupService.revokeInvitation(id, invitationId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id, @PathVariable UUID memberId,
            Authentication authentication) {
        UUID requesterId = UUID.fromString(authentication.getName());
        groupService.removeMember(id, memberId, requesterId);
        return ResponseEntity.noContent().build();
    }
}
