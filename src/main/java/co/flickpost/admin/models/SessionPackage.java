package co.flickpost.admin.models;

import co.flickpost.admin.helpers.ImageInfoConverter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "session_package")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class SessionPackage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "_id")
    @JsonProperty("id")
    private Long id;

    @Column(name = "hub_symbol")
    @JsonProperty("hub")
    private String hub;

    @Column(name = "tracking_number")
    @JsonProperty("code")
    private String trackingNumber;

    @Column(name = "audited_length", scale = 2)
    @JsonProperty("L")
    private BigDecimal auditedLength;

    @Column(name = "audited_width", scale = 2)
    @JsonProperty("W")
    private BigDecimal auditedWidth;

    @Column(name = "audited_height", scale = 2)
    @JsonProperty("H")
    private BigDecimal auditedHeight;

    @Column(name = "audited_weight", scale = 2)
    @JsonProperty("weight")
    private BigDecimal auditedWeight;

    @Column(name = "audited_volumetric_weight", scale = 2)
    @JsonProperty("audited_volumetric_weight")
    private BigDecimal auditedVolumetricWeight;

    @Column(name = "audited_chargeable_weight", scale = 1)
    @JsonProperty("audited_chargeable_weight")
    private BigDecimal auditedChargeableWeight;

    @Column
    @JsonProperty("hid")
    private String hid;

    @Column
    @JsonProperty("status")
    private String status;

    @Column(name = "date_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = JsonFormat.DEFAULT_TIMEZONE)
    @JsonProperty("time")
    private LocalDateTime dateTime;

    @Transient
    @JsonInclude
    @JsonProperty("image")
    private String encodedImage;

    @Column(name = "images")
    @JsonInclude
    @JsonProperty("images")
    @Convert(converter = ImageInfoConverter.class)
    private List<ImageInfo> imageInfos;

    @Column(name = "declared_actual_weight", scale = 2)
    @JsonProperty("declaredActualWeight")
    private BigDecimal declaredActualWeight;

    @Column(name = "declared_chargeable_weight", scale = 1)
    @JsonProperty("declaredChargeableWeight")
    private BigDecimal declaredChargeableWeight;

    @Column(name = "declared_volumetric_weight", scale = 2)
    @JsonProperty("declaredVolumetricWeight")
    private BigDecimal declaredVolumetricWeight;

    @Column(name = "declared_length", scale = 2)
    @JsonProperty("declaredLength")
    private BigDecimal declaredLength;

    @Column(name = "declared_width", scale = 2)
    @JsonProperty("declaredWidth")
    private BigDecimal declaredWidth;

    @Column(name = "declared_height", scale = 2)
    @JsonProperty("declaredHeight")
    private BigDecimal declaredHeight;

    @Column(name = "client_paid_weight", scale = 2)
    @JsonProperty("clientPaidWeight")
    private BigDecimal clientPaidWeight;

    @Column(name = "destination_country")
    @JsonProperty("destinationCountry")
    private String destinationCountry;

    @Column(name = "item_condition")
    @JsonProperty("itemCondition")
    private String itemCondition;

    @Column(name = "contains_liquid")
    @JsonProperty("containsLiquid")
    private Boolean containsLiquid;

    @Column(name = "contains_battery")
    @JsonProperty("containsBattery")
    private Boolean containsBattery;

    @Column(name = "is_commercial_packaging")
    @JsonProperty("isCommercialPackaging")
    private Boolean isCommercialPackaging;

    @Column(name = "shipping_mode")
    @JsonProperty("shippingMode")
    private String shippingMode;

    @Column(name = "service_provider_name")
    @JsonProperty("serviceProviderName")
    private String serviceProviderName;

    @Column(name = "shipment_id")
    @JsonProperty("shipmentId")
    private String shipmentId;

    @Column(name = "session_id")
    @JsonProperty("sessionId")
    private Long sessionId;

    @Column(name = "reference_source")
    @JsonProperty("refSource")
    private String referenceSource;

    @Column(name = "is_underdeclared")
    @JsonProperty("isUnderdeclared")
    private Boolean isUnderdeclared;
}
