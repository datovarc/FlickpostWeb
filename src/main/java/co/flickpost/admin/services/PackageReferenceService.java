package co.flickpost.admin.services;

import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.models.PackageReference;
import co.flickpost.admin.models.json.ThirdPartyParcelDetailsResponse;
import co.flickpost.admin.repositories.PackageReferenceDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class PackageReferenceService {

    private static final Logger logger = LogManager.getLogger(PackageReferenceService.class);

    @Autowired
    private PackageReferenceDao packageReferenceDao;

    @Autowired
    private FlickPostProperties properties;

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<PackageReference> fetchAndUpsert(String trackingNumber) {
        try {
            String requestUrl = properties.getThirdPartyParcelDetailsUrl();
            if (requestUrl == null || requestUrl.trim().isEmpty()) {
                logger.warn("Third-party parcel details URL is not configured. Skipping reference enrichment for {}", trackingNumber);
                return Optional.empty();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", properties.getThirdPartyApiKey());
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<ThirdPartyParcelDetailsResponse> response = restTemplate.exchange(
                    requestUrl + "/" + trackingNumber,
                    HttpMethod.GET,
                    requestEntity,
                    ThirdPartyParcelDetailsResponse.class
            );

            ThirdPartyParcelDetailsResponse body = response.getBody();
            if (body == null || body.getData() == null || body.getData().getData() == null || !Boolean.TRUE.equals(body.getData().getSuccess())) {
                logger.warn("Third-party parcel details response was incomplete or unsuccessful for {}", trackingNumber);
                return Optional.empty();
            }

            PackageReference packageReference = mapToPackageReference(body.getData().getData());
            packageReferenceDao.save(packageReference);
            logger.info("Stored package reference for {}", trackingNumber);
            return Optional.of(packageReference);
        } catch (Exception exception) {
            logger.warn("Failed to fetch/store package reference for {}. Proceeding without enrichment.", trackingNumber, exception);
            return Optional.empty();
        }
    }

    private PackageReference mapToPackageReference(ThirdPartyParcelDetailsResponse.ParcelData parcelData) {
        PackageReference packageReference = new PackageReference();
        packageReference.setTrackingNumber(parcelData.getTrackingNumber());
        packageReference.setShipmentId(parcelData.getShipmentID());
        packageReference.setDeclaredWeight(parcelData.getDeclaredActualWeight());
        packageReference.setDeclaredChargeableWeight(parcelData.getDeclaredChargeableWeight());
        packageReference.setDeclaredLength(parcelData.getDeclaredLength());
        packageReference.setDeclaredHeight(parcelData.getDeclaredHeight());
        packageReference.setDeclaredWidth(parcelData.getDeclaredWidth());
        packageReference.setClientPaidHeight(parcelData.getClientPaidWeight());
        packageReference.setDestinationCountry(parcelData.getDestinationCountry());
        packageReference.setShippingMode(parcelData.getShipmentMode());

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
