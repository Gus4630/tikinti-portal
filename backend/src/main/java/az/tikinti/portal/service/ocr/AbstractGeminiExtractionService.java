package az.tikinti.portal.service.ocr;

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
        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> inlineData = Map.of("mime_type", mimeType, "data", base64Data);
        Map<String, Object> filePart = Map.of("inline_data", inlineData);
        Map<String, Object> content = Map.of("parts", List.of(textPart, filePart));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        generationConfig.put("temperature", 0.0);

        return Map.of("contents", List.of(content), "generation_config", generationConfig);
    }

    private String buildCategoryList(List<CategoryEntity> categories) {
        StringBuilder sb = new StringBuilder();
        categories.forEach(c -> sb
                .append(c.getItemCode()).append(" | ")
                .append(c.getItemName())
                .append(c.getItemDescription() != null ? " — " + c.getItemDescription() : "")
                .append("\n"));
        return sb.toString();
    }

    private String buildPrompt(String categoryList) {
        return """
                You are an invoice OCR extractor for a construction expense tracker.
                Extract the following fields from the invoice image and return ONLY valid JSON, no markdown.

                Available category codes (item_code | name — description):
                %s

                Return exactly this JSON schema:
                {
                  "amount": 0.0,
                  "currency": "AZN",
                  "invoice_date": "YYYY-MM-DD",
                  "supplier_name": "string",
                  "supplier_tax_or_national_id": "string or null",
                  "line_items": [{"description": "string", "quantity": 0, "unit_price": 0.0}],
                  "suggested_category_code": "item_code from the list above",
                  "confidence": {"amount": 0.0, "currency": 0.0, "supplier_name": 0.0, "category": 0.0}
                }

                Rules:
                - amount: total invoice amount as a number
                - currency: 3-letter ISO code (AZN, USD, EUR, etc.)
                - invoice_date: use YYYY-MM-DD format; if unclear use today's date
                - suggested_category_code: must be one of the item_codes from the list above
                - confidence: float 0.0-1.0 per field; use 0.5 if uncertain
                - Return ONLY JSON, no explanation, no markdown fences
                """.formatted(categoryList);
    }

    private OcrExtractionResult parseResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            JsonNode json = objectMapper.readTree(text);

            BigDecimal amount = json.has("amount") && !json.get("amount").isNull()
                    ? new BigDecimal(json.get("amount").asText()) : BigDecimal.ZERO;
            String currency = json.path("currency").asText("AZN").toUpperCase();

            LocalDate invoiceDate;
            try {
                invoiceDate = LocalDate.parse(json.path("invoice_date").asText());
            } catch (Exception e) {
                invoiceDate = LocalDate.now();
            }

            String supplierName = json.path("supplier_name").asText(null);
            String taxId = json.path("supplier_tax_or_national_id").isNull() ? null
                    : json.path("supplier_tax_or_national_id").asText(null);
            String categoryCode = json.path("suggested_category_code").asText(null);

            List<OcrExtractionResult.LineItem> lineItems = new ArrayList<>();
            JsonNode items = json.path("line_items");
            if (items.isArray()) {
                items.forEach(item -> lineItems.add(new OcrExtractionResult.LineItem(
                        item.path("description").asText(),
                        item.path("quantity").asInt(1),
                        new BigDecimal(item.path("unit_price").asText("0"))
                )));
            }

            Map<String, Double> confidence = new HashMap<>();
            JsonNode conf = json.path("confidence");
            if (conf.isObject()) {
                conf.fields().forEachRemaining(e -> confidence.put(e.getKey(), e.getValue().asDouble(0.5)));
            }

            return new OcrExtractionResult(amount, currency, invoiceDate, supplierName,
                    taxId, lineItems, categoryCode, confidence);

        } catch (JsonProcessingException e) {
            throw new NonRetryableOcrException("Gemini returned invalid JSON: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new NonRetryableOcrException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }
}
