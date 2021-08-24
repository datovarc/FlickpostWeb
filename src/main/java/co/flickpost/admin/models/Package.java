package co.flickpost.admin.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "package")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Package implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "_id")
    private Long id;

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

    @Lob
    @Column
    @JsonIgnore
    private byte[] image;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public BigDecimal getAuditedLength() {
        return auditedLength;
    }

    public void setAuditedLength(BigDecimal auditedLength) {
        this.auditedLength = auditedLength;
    }

    public BigDecimal getAuditedWidth() {
        return auditedWidth;
    }

    public void setAuditedWidth(BigDecimal auditedWidth) {
        this.auditedWidth = auditedWidth;
    }

    public BigDecimal getAuditedHeight() {
        return auditedHeight;
    }

    public void setAuditedHeight(BigDecimal auditedHeight) {
        this.auditedHeight = auditedHeight;
    }

    public BigDecimal getAuditedWeight() {
        return auditedWeight;
    }

    public void setAuditedWeight(BigDecimal auditedWeight) {
        this.auditedWeight = auditedWeight;
    }

    public BigDecimal getAuditedVolumetricWeight() {
        return auditedVolumetricWeight;
    }

    public void setAuditedVolumetricWeight(BigDecimal auditedVolumetricWeight) {
        this.auditedVolumetricWeight = auditedVolumetricWeight;
    }

    public BigDecimal getChargeableWeight() {
        return chargeableWeight;
    }

    public void setChargeableWeight(BigDecimal chargeableWeight) {
        this.chargeableWeight = chargeableWeight;
    }

    public String getHid() {
        return hid;
    }

    public void setHid(String hid) {
        this.hid = hid;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getEncodedImage() {
        return encodedImage;
    }

    public void setEncodedImage(String encodedImage) {
        this.encodedImage = encodedImage;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Package aPackage = (Package) o;
        return Objects.equals(id, aPackage.id) &&
                Objects.equals(trackingNumber, aPackage.trackingNumber) &&
                Objects.equals(auditedLength, aPackage.auditedLength) &&
                Objects.equals(auditedWidth, aPackage.auditedWidth) &&
                Objects.equals(auditedHeight, aPackage.auditedHeight) &&
                Objects.equals(auditedWeight, aPackage.auditedWeight) &&
                Objects.equals(auditedVolumetricWeight, aPackage.auditedVolumetricWeight) &&
                Objects.equals(chargeableWeight, aPackage.chargeableWeight) &&
                Objects.equals(hid, aPackage.hid) &&
                Objects.equals(dateTime, aPackage.dateTime) &&
                Objects.equals(encodedImage, aPackage.encodedImage) &&
                Arrays.equals(image, aPackage.image);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(id, trackingNumber, auditedLength, auditedWidth, auditedHeight, auditedWeight, auditedVolumetricWeight, chargeableWeight, hid, dateTime, encodedImage);
        result = 31 * result + Arrays.hashCode(image);
        return result;
    }
}
