package az.tikinti.portal.event;

import java.util.UUID;

public record ExpensePendingReviewEvent(UUID expenseId, String buildingName, String categoryName, String amount) {}
