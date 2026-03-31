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
import java.util.Objects;

@Entity
@Table(name = "package")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class Package implements Serializable {

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

    @Column(name = "audited_length", scale = 4)
    @JsonProperty("L")
    private BigDecimal auditedLength;

    @Column(name = "audited_width", scale = 4)
    @JsonProperty("W")
    private BigDecimal auditedWidth;

    @Column(name = "audited_height", scale = 4)
    @JsonProperty("H")
    private BigDecimal auditedHeight;

    @Column(name = "audited_weight", scale = 4)
    @JsonProperty("weight")
    private BigDecimal auditedWeight;

    @Column(name = "audited_volumetric_weight", scale = 4)
    @JsonProperty("audited_volumetric_weight")
    private BigDecimal auditedVolumetricWeight;

    @Column(name = "chargeable_weight", scale = 4)
    @JsonProperty("chargeable_weight")
    private BigDecimal chargeableWeight;

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

    @Column(name = "declared_weight", scale = 2)
    @JsonProperty("declaredWeight")
    private BigDecimal declaredWeight;

    @Column(name = "declared_length", scale = 2)
    @JsonProperty("declaredLength")
    private BigDecimal declaredLength;

    @Column(name = "declared_width", scale = 2)
    @JsonProperty("declaredWidth")
    private BigDecimal declaredWidth;

    @Column(name = "declared_height", scale = 2)
    @JsonProperty("declaredHeight")
    private BigDecimal declaredHeight;

    @Column(name = "client_paid_height", scale = 2)
    @JsonProperty("clientPaidHeight")
    private BigDecimal clientPaidHeight;

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
}
