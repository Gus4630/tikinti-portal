package az.tikinti.portal.service.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiFlashExtractionService extends AbstractGeminiExtractionService {

    private final String modelId;

    public GeminiFlashExtractionService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${application.gemini.api-url}") String apiUrl,
            @Value("${application.gemini.api-key}") String apiKey,
            @Value("${application.gemini.fallback-model}") String modelId) {
        super(restTemplate, objectMapper, apiUrl, apiKey);
        this.modelId = modelId;
    }

    @Override
    protected String getModelId() {
        return modelId;
    }
}
