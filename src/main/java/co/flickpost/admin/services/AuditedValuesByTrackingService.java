package co.flickpost.admin.services;

import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.models.json.AuditedValuesByTrackingPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AuditedValuesByTrackingService {

    private static final Logger logger = LogManager.getLogger(AuditedValuesByTrackingService.class);
    @Autowired
    private FlickPostProperties properties;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean sendAuditedValues(List<AuditedValuesByTrackingPayload> packages) {
        if (packages == null || packages.isEmpty()) {
            throw new IllegalArgumentException("No packages to process");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", properties.getThirdPartyApiKey());

            for (AuditedValuesByTrackingPayload payload : packages) {
                HttpEntity<AuditedValuesByTrackingPayload> requestEntity = new HttpEntity<>(payload, headers);
                ResponseEntity<Map> response = restTemplate.exchange(
                        properties.getThirdPartyAuditedValuesUrl(),
                        HttpMethod.POST,
                        requestEntity,
                        Map.class
                );
                if (!response.getStatusCode().is2xxSuccessful()) {
                    logger.warn("Audited-values API returned non-2xx for trackingNumber={}", payload.getTrackingNumber());
                    return false;
                }
            }
            return true;
        } catch (Exception exception) {
            logger.error("Failed to call audited-values-by-tracking API", exception);
            return false;
        }
    }
}
