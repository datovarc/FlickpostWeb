package co.flickpost.admin.services;

import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.models.PackageReference;
import co.flickpost.admin.models.json.FpReferencePayload;
import co.flickpost.admin.models.json.ThirdPartyParcelDetailsResponse;
import co.flickpost.admin.repositories.PackageReferenceDao;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ThirdPartyParcelDetailsService {

    private static final Logger logger = LogManager.getLogger(ThirdPartyParcelDetailsService.class);

    @Autowired
    private FlickPostProperties properties;

    @Autowired
    private PackageReferenceDao packageReferenceDao;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, PackageReference> fetchAndUpsert(List<String> trackingNumbers) {
        if (trackingNumbers == null || trackingNumbers.isEmpty()) {
            return Collections.emptyMap();
        }

        String requestUrl = properties.getThirdPartyParcelDetailsUrl();
        if (StringUtils.isBlank(requestUrl)) {
            logger.warn("Third-party parcel details URL is not configured. Skipping reference enrichment for {} tracking numbers", trackingNumbers.size());
            return Collections.emptyMap();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", properties.getThirdPartyApiKey());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("trackingNumbers", new ArrayList<>(trackingNumbers));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

            ResponseEntity<ThirdPartyParcelDetailsResponse> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.POST,
                    requestEntity,
                    ThirdPartyParcelDetailsResponse.class
            );

            ThirdPartyParcelDetailsResponse body = response.getBody();
            if (body == null || body.getData() == null || body.getData().getData() == null || !Boolean.TRUE.equals(body.getData().getSuccess())) {
                logger.warn("Third-party parcel details batch response was incomplete or unsuccessful for {} tracking numbers", trackingNumbers.size());
                return Collections.emptyMap();
            }

            Map<String, PackageReference> referencesByTrackingNumber = new LinkedHashMap<>();
            for (ThirdPartyParcelDetailsResponse.ParcelData parcelData : body.getData().getData()) {
                if (parcelData == null || StringUtils.isBlank(parcelData.getTrackingNumber())) {
                    continue;
                }
                PackageReference packageReference = mapToPackageReference(parcelData);
                packageReferenceDao.save(packageReference);
                referencesByTrackingNumber.put(packageReference.getTrackingNumber(), packageReference);
            }

            logger.info("Stored {} package references from batch lookup", referencesByTrackingNumber.size());
            return referencesByTrackingNumber;
        } catch (Exception exception) {
            logger.warn("Failed to fetch/store package references in batch. Proceeding without API enrichment.", exception);
            return Collections.emptyMap();
        }
    }

    public FpReferencePayload toPayload(PackageReference packageReference) {
        if (packageReference == null) {
            return null;
        }
        FpReferencePayload payload = new FpReferencePayload();
        payload.setShipmentId(packageReference.getShipmentId());
        payload.setDeclaredActualWeight(packageReference.getDeclaredActualWeight());
        payload.setDeclaredChargeableWeight(packageReference.getDeclaredChargeableWeight());
        payload.setDeclaredVolumetricWeight(packageReference.getDeclaredVolumetricWeight());
        payload.setDeclaredLength(packageReference.getDeclaredLength());
        payload.setDeclaredWidth(packageReference.getDeclaredWidth());
        payload.setDeclaredHeight(packageReference.getDeclaredHeight());
        payload.setClientPaidWeight(packageReference.getClientPaidWeight());
        payload.setDestinationCountry(packageReference.getDestinationCountry());
        payload.setItemCondition(packageReference.getItemCondition());
        payload.setContainsLiquid(packageReference.getContainsLiquid());
        payload.setContainsBattery(packageReference.getContainsBattery());
        payload.setIsCommercialPackaging(packageReference.getIsCommercialPackaging());
        payload.setShippingMode(packageReference.getShippingMode());
        payload.setServiceProviderName(packageReference.getServiceProviderName());
        return payload;
    }

    private PackageReference mapToPackageReference(ThirdPartyParcelDetailsResponse.ParcelData parcelData) {
        PackageReference existing = packageReferenceDao.findByTrackingNumber(parcelData.getTrackingNumber());
        PackageReference packageReference = existing != null ? existing : new PackageReference();
        packageReference.setTrackingNumber(parcelData.getTrackingNumber());
        packageReference.setShipmentId(parcelData.getShipmentID());
        packageReference.setDeclaredActualWeight(parcelData.getDeclaredActualWeight());
        packageReference.setDeclaredChargeableWeight(parcelData.getDeclaredChargeableWeight());
        packageReference.setDeclaredLength(parcelData.getDeclaredLength());
        packageReference.setDeclaredHeight(parcelData.getDeclaredHeight());
        packageReference.setDeclaredWidth(parcelData.getDeclaredWidth());
        packageReference.setClientPaidWeight(parcelData.getClientPaidWeight());
        packageReference.setDestinationCountry(parcelData.getDestinationCountry());
        packageReference.setShippingMode(parcelData.getShipmentMode());
        packageReference.setDeclaredVolumetricWeight(parcelData.getDeclaredVolumetricWeight());
        packageReference.setServiceProviderName(parcelData.getServiceProviderName());
        packageReference.setTtlTimestamp(LocalDateTime.now());

        applyItemAggregates(packageReference, parcelData.getItems());
        return packageReference;
    }

    private void applyItemAggregates(PackageReference packageReference, List<ThirdPartyParcelDetailsResponse.ParcelItem> items) {
        boolean containsLiquid = false;
        boolean containsBattery = false;
        boolean isCommercialPackaging = true;
        String itemCondition = "New";

        if (items != null) {
            for (ThirdPartyParcelDetailsResponse.ParcelItem item : items) {
                if (item == null) {
                    continue;
                }
                if (Boolean.TRUE.equals(item.getContainLiquid())) {
                    containsLiquid = true;
                }
                if (Boolean.TRUE.equals(item.getContainBattery())) {
                    containsBattery = true;
                }
                if (!Boolean.TRUE.equals(item.getCommercialPackaging())) {
                    isCommercialPackaging = false;
                }
                if (item.getItemCondition() != null && "used".equalsIgnoreCase(item.getItemCondition().trim())) {
                    itemCondition = "Used";
                }
            }
        }

        packageReference.setContainsLiquid(containsLiquid);
        packageReference.setContainsBattery(containsBattery);
        packageReference.setIsCommercialPackaging(isCommercialPackaging);
        packageReference.setItemCondition(itemCondition);
    }
}
