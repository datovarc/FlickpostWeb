package co.flickpost.admin.models.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThirdPartyParcelDetailsResponse {

    private ResponseData data;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseData {
        private Boolean success;
        private ParcelData data;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParcelData {
        private String trackingNumber;
        private String shipmentID;
        private BigDecimal declaredActualWeight;
        private BigDecimal declaredChargeableWeight;
        private BigDecimal declaredVolumetricWeight;
        private BigDecimal declaredLength;
        private BigDecimal declaredHeight;
        private BigDecimal declaredWidth;
        private BigDecimal clientPaidWeight;
        private String destinationCountry;
        private List<ParcelItem> items;
        private String shipmentMode;
        private String serviceProviderName;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParcelItem {
        private String itemCondition;
        private Boolean containLiquid;
        private Boolean containBattery;
        private Boolean commercialPackaging;
    }
}
