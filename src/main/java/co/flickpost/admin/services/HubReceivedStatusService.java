package co.flickpost.admin.services;

import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.models.json.HubReceivedStatusRequest;
import co.flickpost.admin.models.json.HubReceivedStatusResponse;
import org.apache.commons.lang3.StringUtils;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HubReceivedStatusService {

    private static final Logger logger = LogManager.getLogger(HubReceivedStatusService.class);
    @Autowired
    private FlickPostProperties properties;

    private final RestTemplate restTemplate = new RestTemplate();

    public HubReceivedStatusResponse sendHubReceivedStatus(List<String> trackingNumbers) {
        Set<String> uniqueTrackingNumbers = new LinkedHashSet<>();
        if (trackingNumbers != null) {
            for (String trackingNumber : trackingNumbers) {
                if (StringUtils.isNotBlank(trackingNumber)) {
                    uniqueTrackingNumbers.add(trackingNumber.trim());
                }
            }
        }

        if (uniqueTrackingNumbers.isEmpty()) {
            throw new IllegalArgumentException("No tracking numbers to process");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", properties.getThirdPartyApiKey());

            HubReceivedStatusRequest payload = new HubReceivedStatusRequest();
            payload.setTrackingNumbers(new ArrayList<>(uniqueTrackingNumbers));

            HttpEntity<HubReceivedStatusRequest> requestEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.getThirdPartyHubReceivedUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("Hub-received status API returned non-2xx: {}", response.getStatusCode());
                return new HubReceivedStatusResponse(false, "Failed to add status", uniqueTrackingNumbers.size());
            }

            return new HubReceivedStatusResponse(true, "Status added successfully", uniqueTrackingNumbers.size());
        } catch (Exception exception) {
            logger.error("Failed to call hub-received status API", exception);
            return new HubReceivedStatusResponse(false, "Failed to add status", uniqueTrackingNumbers.size());
        }
    }
}
