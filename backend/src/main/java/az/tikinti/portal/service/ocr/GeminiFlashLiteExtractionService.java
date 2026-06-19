package az.tikinti.portal.service.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiFlashLiteExtractionService extends AbstractGeminiExtractionService {

    private static final String MODEL_ID = "gemini-3.1-flash-lite";

    public GeminiFlashLiteExtractionService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${application.gemini.api-url}") String apiUrl,
            @Value("${application.gemini.api-key}") String apiKey) {
        super(restTemplate, objectMapper, apiUrl, apiKey);
    }

    @Override
    protected String getModelId() {
        return MODEL_ID;
    }
}
