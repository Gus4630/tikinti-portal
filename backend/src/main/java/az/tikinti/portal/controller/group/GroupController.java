package az.tikinti.portal.controller.group;

import static org.springframework.http.HttpStatus.CREATED;

import az.tikinti.portal.model.dto.request.group.AddGroupMemberRequest;
import az.tikinti.portal.model.dto.request.group.GroupRequest;
import az.tikinti.portal.model.dto.response.group.GroupMemberResponse;
import az.tikinti.portal.model.dto.response.group.GroupResponse;
import az.tikinti.portal.service.group.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<GroupResponse>> findAll() {
        return ResponseEntity.ok(groupService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(groupService.getById(id));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupRequest request) {
        return ResponseEntity.status(CREATED).body(groupService.create(request));
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

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupMemberResponse> addMember(
            @PathVariable UUID id, @Valid @RequestBody AddGroupMemberRequest request) {
        return ResponseEntity.status(CREATED).body(groupService.addMember(id, request));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id, @PathVariable UUID memberId) {
        groupService.removeMember(id, memberId);
        return ResponseEntity.noContent().build();
    }
}
