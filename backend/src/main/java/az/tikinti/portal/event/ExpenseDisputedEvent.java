package az.tikinti.portal.event;

import java.util.UUID;

public record ExpenseDisputedEvent(UUID expenseId, String buildingName, String categoryName, String amount, String reason) {}
