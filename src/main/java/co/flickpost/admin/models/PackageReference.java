package co.flickpost.admin.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "package_reference")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class PackageReference implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "_id")
    private Long id;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "shipment_id")
    private String shipmentId;

    @Column(name = "declared_actual_weight", scale = 2)
    private BigDecimal declaredActualWeight;

    @Column(name = "declared_volumetric_weight", scale = 2)
    private BigDecimal declaredVolumetricWeight;

    @Column(name = "declared_chargeable_weight", scale = 1)
    private BigDecimal declaredChargeableWeight;

    @Column(name = "declared_length", scale = 2)
    private BigDecimal declaredLength;

    @Column(name = "declared_height", scale = 2)
    private BigDecimal declaredHeight;

    @Column(name = "declared_width", scale = 2)
    private BigDecimal declaredWidth;

    @Column(name = "client_paid_weight", scale = 2)
    private BigDecimal clientPaidWeight;

    @Column(name = "destination_country")
    private String destinationCountry;

    @Column(name = "item_condition")
    private String itemCondition;

    @Column(name = "contains_liquid")
    private Boolean containsLiquid;

    @Column(name = "contains_battery")
    private Boolean containsBattery;

    @Column(name = "is_commercial_packaging")
    private Boolean isCommercialPackaging;

    @Column(name = "shipping_mode")
    private String shippingMode;

    @Column(name = "service_provider_name")
    private String serviceProviderName;

    @Column(name = "ttl_timestamp")
    private LocalDateTime ttlTimestamp;
}
