package az.tikinti.portal.controller.auth;

import static org.springframework.http.HttpStatus.CREATED;

import az.tikinti.portal.model.dto.request.auth.LoginRequest;
import az.tikinti.portal.model.dto.request.auth.RefreshTokenRequest;
import az.tikinti.portal.model.dto.request.auth.RegisterRequest;
import az.tikinti.portal.model.dto.response.auth.AuthResponse;
import az.tikinti.portal.model.dto.response.auth.UserResponse;
import az.tikinti.portal.model.dto.record.SessionResponse;
import az.tikinti.portal.model.dto.response.group.GroupInvitationResponse;
import az.tikinti.portal.service.auth.AuthService;
import az.tikinti.portal.service.group.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final GroupService groupService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(authService.getMe(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @RequestBody Map<String, String> body) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(authService.updateProfile(userId, body.get("fullName")));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(authService.listSessions(userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        authService.revokeSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> revokeAllSessions(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        authService.revokeAllSessions(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/invitations")
    public ResponseEntity<List<GroupInvitationResponse>> getMyInvitations(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(groupService.getMyInvitations(userId));
    }

    @PostMapping("/me/invitations/{id}/accept")
    public ResponseEntity<Void> acceptInvitation(@PathVariable UUID id, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        groupService.acceptInvitation(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/invitations/{id}/decline")
    public ResponseEntity<Void> declineInvitation(@PathVariable UUID id, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        groupService.declineInvitation(id, userId);
        return ResponseEntity.noContent().build();
    }
}
