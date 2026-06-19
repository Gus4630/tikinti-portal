package az.tikinti.portal.service.notification;

import az.tikinti.portal.event.BudgetThresholdEvent;
import az.tikinti.portal.event.ExpenseDisputedEvent;
import az.tikinti.portal.event.ExpensePendingReviewEvent;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class TelegramNotificationService {

    private static final String TELEGRAM_API = "https://api.telegram.org/bot%s/sendMessage";

    private final RestTemplate restTemplate;
    private final String botToken;
    private final String chatId;

    public TelegramNotificationService(
            RestTemplate restTemplate,
            @Value("${application.telegram.bot-token:}") String botToken,
            @Value("${application.telegram.chat-id:}") String chatId) {
        this.restTemplate = restTemplate;
        this.botToken = botToken;
        this.chatId = chatId;
    }

    @Async
    @EventListener
    public void onPendingReview(ExpensePendingReviewEvent event) {
        sendMessage("""
                *New Expense Pending Review*
                Building: %s
                Category: %s
                Amount: %s AZN
                ID: `%s`
                """.formatted(event.buildingName(), event.categoryName(), event.amount(), event.expenseId()));
    }

    @Async
    @EventListener
    public void onDisputed(ExpenseDisputedEvent event) {
        sendMessage("""
                *Expense Flagged as DISPUTED*
                Building: %s
                Category: %s
                Amount: %s AZN
                Reason: %s
                ID: `%s`
                """.formatted(event.buildingName(), event.categoryName(), event.amount(),
                event.reason() != null ? event.reason() : "anomaly detected", event.expenseId()));
    }

    @Async
    @EventListener
    public void onBudgetThreshold(BudgetThresholdEvent event) {
        sendMessage("""
                *Budget Threshold Reached: %d%%*
                Building: %s
                Category: %s (%s)
                Budget: %s AZN
                Spent: %s AZN
                """.formatted(event.thresholdPercent(), event.buildingName(),
                event.categoryName(), event.categoryCode(),
                event.budgetLimit().toPlainString(), event.actualSpend().toPlainString()));
    }

    private void sendMessage(String text) {
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.debug("Telegram not configured, skipping notification");
            return;
        }
        try {
            String url = TELEGRAM_API.formatted(botToken);
            Map<String, String> body = Map.of("chat_id", chatId, "text", text, "parse_mode", "Markdown");
            ResponseEntity<String> response = restTemplate.postForEntity(url, body, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Telegram notification failed: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Telegram notification error: {}", e.getMessage());
        }
    }
}
