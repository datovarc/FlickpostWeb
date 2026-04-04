package co.flickpost.admin.models.json;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class FpSessionPackageIngestRequest {
    private Boolean isFilled;
    private String trackingNumber;
    private String hub;
    private BigDecimal auditedLength;
    private BigDecimal auditedWidth;
    private BigDecimal auditedHeight;
    private BigDecimal auditedWeight;
    private BigDecimal auditedVolumetricWeight;
    private BigDecimal chargeableWeight;
    private String hid;
    private LocalDateTime dateTime;
    private Long sessionId;
    private FpReferencePayload reference;
}
