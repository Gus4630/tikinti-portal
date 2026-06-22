package az.tikinti.portal.service.ocr;

import az.tikinti.portal.aop.NoLog;
import az.tikinti.portal.dao.entity.category.CategoryEntity;
import az.tikinti.portal.exception.NonRetryableOcrException;
import az.tikinti.portal.model.dto.record.OcrExtractionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@NoLog
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractGeminiExtractionService implements InvoiceExtractionService {

    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;
    protected final String apiUrl;
    protected final String apiKey;

    protected abstract String getModelId();

    @Override
    public OcrExtractionResult extract(byte[] fileBytes, String mimeType, List<CategoryEntity> categories) {
        String base64File = Base64.getEncoder().encodeToString(fileBytes);
        String categoryList = buildCategoryList(categories);
        String prompt = buildPrompt(categoryList);
        String url = apiUrl + "/models/" + getModelId() + ":generateContent?key=" + apiKey;

        Map<String, Object> body = buildRequestBody(prompt, base64File, mimeType);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String raw;
        try {
            raw = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
        } catch (HttpClientErrorException e) {
            int code = e.getStatusCode().value();
            if (code == 429) {
                log.warn("Gemini rate limit on model {} — will try fallback: {}", getModelId(), e.getMessage());
                throw new RuntimeException("Gemini rate limit: " + e.getMessage(), e);
            }
            log.error("Gemini API client error on model {} ({}): {}", getModelId(), e.getStatusCode(), e.getMessage());
            throw new NonRetryableOcrException("Gemini API error " + e.getStatusCode() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Gemini API call failed on model {}: {}", getModelId(), e.getMessage());
            throw new RuntimeException("Gemini API transient error: " + e.getMessage(), e);
        }

        return parseResponse(raw);
    }

    private Map<String, Object> buildRequestBody(String prompt, String base64Data, String mimeType) {
        Map<String, Object> inlineData = Map.of("mime_type", mimeType, "data", base64Data);
        Map<String, Object> filePart = Map.of("inline_data", inlineData);
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(filePart, textPart));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        generationConfig.put("temperature", 0.0);

        return Map.of("contents", List.of(content), "generation_config", generationConfig);
    }

    private String buildCategoryList(List<CategoryEntity> categories) {
        var sb = new StringBuilder();
        categories.forEach(c -> sb
                .append(c.getItemCode()).append(" | ")
                .append(c.getItemName())
                .append(c.getItemDescription() != null ? " — " + c.getItemDescription() : "")
                .append("\n"));
        return sb.toString();
    }

    private String buildPrompt(String categoryList) {
        return """
                You are an OCR data extractor for a construction expense management system.
                Analyse the attached invoice or receipt image and extract the fields listed below.
                Respond with ONLY a single valid JSON object — no markdown, no code fences, no explanation.

                CATEGORY LIST (item_code | name — description):
                %s

                REQUIRED JSON STRUCTURE:
                {
                  "amount": <total invoice amount as a decimal number, e.g. 1250.00>,
                  "currency": <3-letter ISO 4217 code, e.g. "AZN", "USD", "EUR">,
                  "invoice_date": <date in YYYY-MM-DD format; convert DD.MM.YYYY or other formats if needed>,
                  "supplier_name": <company or person name on the invoice, or null if not present>,
                  "supplier_tax_or_national_id": <VÖEN / tax ID / national ID if visible, or null>,
                  "line_items": [
                    {"description": <item name>, "quantity": <integer count, minimum 1>, "unit_price": <price per unit as decimal>}
                  ],
                  "suggested_category_code": <item_code from the CATEGORY LIST above that best matches this invoice, or null if none fits>,
                  "confidence": {
                    "amount": <0.0-1.0>,
                    "currency": <0.0-1.0>,
                    "supplier_name": <0.0-1.0>,
                    "category": <0.0-1.0>
                  }
                }

                RULES:
                - All monetary values must be plain decimal numbers without currency symbols or thousand separators.
                - If a field is not present in the document, use null (not an empty string).
                - line_items must include every distinct product or service line; if no line items are visible, use an empty array [].
                - suggested_category_code MUST exactly match one of the item_codes in the CATEGORY LIST or be null.
                - confidence values reflect how certain you are: 1.0 = clearly visible, 0.5 = inferred, 0.0 = guessed.
                - Return ONLY the JSON object. Do not wrap it in markdown fences or add any text outside the JSON.
                """.formatted(categoryList);
    }

    private OcrExtractionResult parseResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            text = stripMarkdownFences(text);

            JsonNode json = objectMapper.readTree(text);

            BigDecimal amount = parseBigDecimal(json.path("amount").asText("0"));
            String currency = json.path("currency").asText("AZN").toUpperCase();

            LocalDate invoiceDate;
            try {
                invoiceDate = LocalDate.parse(json.path("invoice_date").asText());
            } catch (Exception e) {
                invoiceDate = LocalDate.now();
            }

            String supplierName = nullableText(json, "supplier_name");
            String taxId = nullableText(json, "supplier_tax_or_national_id");
            String categoryCode = nullableText(json, "suggested_category_code");

            List<OcrExtractionResult.LineItem> lineItems = new ArrayList<>();
            JsonNode items = json.path("line_items");
            if (items.isArray()) {
                items.forEach(item -> {
                    String description = item.path("description").asText("");
                    int quantity = Math.max(1, (int) Math.round(item.path("quantity").asDouble(1)));
                    BigDecimal unitPrice = parseBigDecimal(item.path("unit_price").asText("0"));
                    lineItems.add(new OcrExtractionResult.LineItem(description, quantity, unitPrice));
                });
            }

            Map<String, Double> confidence = new HashMap<>();
            JsonNode conf = json.path("confidence");
            if (conf.isObject()) {
                conf.fields().forEachRemaining(e -> confidence.put(e.getKey(), e.getValue().asDouble(0.5)));
            }

            return new OcrExtractionResult(amount, currency, invoiceDate, supplierName,
                    taxId, lineItems, categoryCode, confidence);

        } catch (JsonProcessingException e) {
            log.error("Gemini returned invalid JSON from model {}: {}", getModelId(), e.getMessage());
            throw new NonRetryableOcrException("Gemini returned invalid JSON: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to parse Gemini response from model {}: {}", getModelId(), e.getMessage());
            throw new NonRetryableOcrException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    private String stripMarkdownFences(String text) {
        if (text == null) return "";
        text = text.strip();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```[a-zA-Z]*\\r?\\n?", "");
            int closing = text.lastIndexOf("```");
            if (closing >= 0) {
                text = text.substring(0, closing);
            }
            text = text.strip();
        }
        return text;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) return BigDecimal.ZERO;
        try {
            // Remove thousand separators (spaces, commas when used as thousands) but keep decimal point/comma
            String cleaned = value.replaceAll("[\\s]", "").replace(",", ".");
            // If there are multiple dots after cleaning, keep only the last one
            int lastDot = cleaned.lastIndexOf('.');
            if (lastDot >= 0) {
                cleaned = cleaned.substring(0, lastDot).replace(".", "") + "." + cleaned.substring(lastDot + 1);
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Could not parse BigDecimal from '{}', defaulting to 0", value);
            return BigDecimal.ZERO;
        }
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return null;
        String val = n.asText(null);
        return (val == null || val.isBlank() || "null".equalsIgnoreCase(val)) ? null : val;
    }
}
