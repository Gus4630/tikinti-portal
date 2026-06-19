package az.tikinti.portal.model.dto.record;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String deviceLabel,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {}
