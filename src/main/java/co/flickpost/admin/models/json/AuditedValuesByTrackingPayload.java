package co.flickpost.admin.models.json;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AuditedValuesByTrackingPayload {
    private String trackingNumber;
    private BigDecimal auditedActualWeight;
    private BigDecimal auditedLength;
    private BigDecimal auditedWidth;
    private BigDecimal auditedHeight;
    private BigDecimal auditedVolumetricWeight;
    private BigDecimal auditedChargeableWeight;
    private List<String> dwsPictures;
    private String hubID;
    private String hubSymbol;
    private String dateTime;
}
