package co.flickpost.admin.models.json;

import co.flickpost.admin.models.ImageInfo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class FpSessionPackageIngestRequest {
    private Boolean isFilled;
    private String trackingNumber;
    private String hub;
    private BigDecimal auditedLength;
    private BigDecimal auditedWidth;
    private BigDecimal auditedHeight;
    private BigDecimal auditedActualWeight;
    private BigDecimal auditedVolumetricWeight;
    private BigDecimal auditedChargeableWeight;
    private String hid;
    private LocalDateTime dateTime;
    private Long sessionId;
    private List<ImageInfo> imageInfos;
    private FpReferencePayload reference;
}
