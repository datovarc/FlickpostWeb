package co.flickpost.admin.models.json;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FpReferencePayload {
    private String shipmentId;
    private BigDecimal declaredWeight;
    private BigDecimal declaredLength;
    private BigDecimal declaredWidth;
    private BigDecimal declaredHeight;
    private BigDecimal clientPaidHeight;
    private String itemCondition;
    private Boolean containsLiquid;
    private Boolean containsBattery;
    private Boolean isCommercialPackaging;
    private String shippingMode;
}
