package co.flickpost.admin.models.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionPackageEvaluationRequest {

    private Boolean isFilled;
    private String trackingNumber;
    private String shippingMode;
    private Boolean containsLiquid;
    private Boolean containsBattery;
    private String itemCondition;
    private Boolean isCommercialPackaging;
    private BigDecimal declaredWeight;
    private BigDecimal auditedWeight;
    private BigDecimal auditedVolumetricWeight;
    private BigDecimal declaredLength;
    private BigDecimal declaredWidth;
    private BigDecimal declaredHeight;
    private BigDecimal clientPaidHeight;
    private String destinationCountry;
    private String shipmentId;
}
