package az.tikinti.portal.service.auth;

import static az.tikinti.portal.exception.model.constant.ErrorMessage.ALREADY_EXISTS_MESSAGE;
import static az.tikinti.portal.exception.model.constant.ErrorMessage.INVALID_CREDENTIALS_MESSAGE;
import static az.tikinti.portal.exception.model.constant.ErrorMessage.REFRESH_TOKEN_INVALID_MESSAGE;
import static az.tikinti.portal.exception.model.constant.ErrorMessage.USER_NOT_FOUND_MESSAGE;

import az.tikinti.portal.dao.entity.auth.RefreshTokenEntity;
import az.tikinti.portal.dao.entity.auth.UserEntity;
import az.tikinti.portal.dao.repository.auth.RefreshTokenRepository;
import az.tikinti.portal.dao.repository.auth.UserRepository;
import az.tikinti.portal.exception.AlreadyExistsException;
import az.tikinti.portal.exception.CommonException;
import az.tikinti.portal.exception.DataNotFoundException;
import az.tikinti.portal.exception.model.constant.ErrorCode;
import az.tikinti.portal.model.dto.request.auth.LoginRequest;
import az.tikinti.portal.model.dto.request.auth.RefreshTokenRequest;
import az.tikinti.portal.model.dto.request.auth.RegisterRequest;
import az.tikinti.portal.model.dto.response.auth.AuthResponse;
import az.tikinti.portal.model.dto.record.SessionResponse;
import az.tikinti.portal.model.dto.response.auth.UserResponse;
import az.tikinti.portal.model.enums.UserRole;
import az.tikinti.portal.util.HashUtil;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.jwt.refresh-expiry-days:30}")
    private long refreshExpiryDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw AlreadyExistsException.of(ALREADY_EXISTS_MESSAGE, "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AlreadyExistsException.of(ALREADY_EXISTS_MESSAGE, "email", request.getEmail());
        }

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();
        user = userRepository.save(user);

        log.info("Registered new user: {}", user.getUsername());
        return buildAuthResponse(user, request.getDeviceLabel());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CommonException(ErrorCode.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        log.info("User logged in: {}", user.getUsername());
        return buildAuthResponse(user, request.getDeviceLabel());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = HashUtil.sha256Hex(request.getRefreshToken().getBytes(StandardCharsets.UTF_8));
        RefreshTokenEntity token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CommonException(ErrorCode.UNAUTHORIZED, REFRESH_TOKEN_INVALID_MESSAGE));

        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CommonException(ErrorCode.UNAUTHORIZED, REFRESH_TOKEN_INVALID_MESSAGE);
        }

        // Rotate: revoke old, issue new
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);

        return buildAuthResponse(token.getUser(), token.getDeviceLabel());
    }

    @Transactional
    public void logout(String tokenHash) {
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(t -> {
                    t.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(t);
                });
    }

    @Transactional
    public void revokeSession(UUID userId, UUID sessionId) {
        RefreshTokenEntity token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> DataNotFoundException.of("Session {0} not found", sessionId));
        if (!token.getUser().getId().equals(userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN, "Session does not belong to the current user");
        }
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void revokeAllSessions(UUID userId) {
        int count = refreshTokenRepository.revokeAllForUser(userId, LocalDateTime.now());
        log.info("Revoked {} sessions for user {}", count, userId);
    }

    public List<SessionResponse> listSessions(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        return refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId).stream()
                .filter(t -> t.getExpiresAt().isAfter(now))
                .map(t -> new SessionResponse(t.getId(), t.getDeviceLabel(),
                        t.getCreatedAt(), t.getExpiresAt()))
                .toList();
    }

    private AuthResponse buildAuthResponse(UserEntity user, String deviceLabel) {
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = HashUtil.sha256Hex(rawRefreshToken.getBytes(StandardCharsets.UTF_8));

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(tokenHash)
                .deviceLabel(deviceLabel)
                .expiresAt(LocalDateTime.now().plusDays(refreshExpiryDays))
                .build();
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .accessExpiresInSeconds(jwtService.getAccessExpirySeconds())
                .user(UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .build())
                .toList();
    }

    public UserResponse getMe(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> DataNotFoundException.of(USER_NOT_FOUND_MESSAGE, "id", userId));
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, String fullName) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> DataNotFoundException.of(USER_NOT_FOUND_MESSAGE, "id", userId));
        user.setFullName(fullName);
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
