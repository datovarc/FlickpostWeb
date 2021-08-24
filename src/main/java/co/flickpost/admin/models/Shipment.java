package co.flickpost.admin.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "shipment")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Shipment implements Serializable {


    @Id
    @Column(name = "trackingNo", columnDefinition = "varchar(50)")
    @JsonProperty("trackingNo")
    private String trackingNumber;

    @Column(name = "referenceNo", columnDefinition = "varchar(50)")
    @JsonProperty("referenceNo")
    private String referenceNumber;

    @Column
    @JsonProperty
    private Long customerSystemNumber;

    @Column
    @JsonProperty
    private Long customerAccountNumber;

    @Column(columnDefinition = "varchar(100)")
    @JsonProperty
    private String customerName;

    @Column(columnDefinition = "varchar(100)")
    @JsonProperty
    private String senderName;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String senderPhone;

    @Column(columnDefinition = "varchar(100)")
    @JsonProperty
    private String senderStreetAddress;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String senderCity;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String senderState;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String senderCountry;

    @Column
    @JsonProperty
    private Long senderCode;

    @Column(columnDefinition = "varchar(100)")
    @JsonProperty
    private String receiverName;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String receiverEmail;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String receiverPhone;

    @Column(columnDefinition = "varchar(100)")
    @JsonProperty
    private String receiverStreetAddress;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String receiverCity;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String receiverState;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String receiverCountry;

    @Column
    @JsonProperty
    private Long receiverCode;

    @Column
    @JsonProperty
    private Integer itemsCount;

    @Column(columnDefinition = "text")
    @JsonProperty
    private String description;

    @Column
    @JsonProperty
    private Integer quantity;

    @Column
    @JsonProperty
    private BigDecimal weight;

    @Column
    @JsonProperty
    private BigDecimal declaredValue;

    @Column(columnDefinition = "text")
    @JsonProperty
    private String pickupInstructions;

    @Column
    @JsonProperty
    private BigDecimal height;

    @Column
    @JsonProperty
    private BigDecimal width;

    @Column
    @JsonProperty
    private BigDecimal scale;

    @Column
    @JsonProperty
    private BigDecimal dimensionalWeight;

    @Column(columnDefinition = "varchar(30)")
    @JsonProperty
    private String type;

    @Column(columnDefinition = "varchar(30)")
    @JsonProperty
    private String mode;

    @Column(columnDefinition = "varchar(50)")
    @JsonProperty
    private String shipmentStatus;

    @Column
    @JsonProperty
    private String shippingCost;

    @Column(columnDefinition = "varchar(100)")
    @JsonProperty
    private String costBreakdown;

    @Column(columnDefinition = "varchar(20)")
    @JsonProperty
    private String paymentStatus;

    @Column(name = "bookingDate")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = JsonFormat.DEFAULT_TIMEZONE)
    @JsonProperty("bookingDate")
    private LocalDateTime bookingDate;

    @Column(name = "deliveryDate")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = JsonFormat.DEFAULT_TIMEZONE)
    @JsonProperty("deliveryDate")
    private LocalDateTime deliveryDate;

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Long getCustomerSystemNumber() {
        return customerSystemNumber;
    }

    public void setCustomerSystemNumber(Long customerSystemNumber) {
        this.customerSystemNumber = customerSystemNumber;
    }

    public Long getCustomerAccountNumber() {
        return customerAccountNumber;
    }

    public void setCustomerAccountNumber(Long customerAccountNumber) {
        this.customerAccountNumber = customerAccountNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderPhone() {
        return senderPhone;
    }

    public void setSenderPhone(String senderPhone) {
        this.senderPhone = senderPhone;
    }

    public String getSenderStreetAddress() {
        return senderStreetAddress;
    }

    public void setSenderStreetAddress(String senderStreetAddress) {
        this.senderStreetAddress = senderStreetAddress;
    }

    public String getSenderCity() {
        return senderCity;
    }

    public void setSenderCity(String senderCity) {
        this.senderCity = senderCity;
    }

    public String getSenderState() {
        return senderState;
    }

    public void setSenderState(String senderState) {
        this.senderState = senderState;
    }

    public String getSenderCountry() {
        return senderCountry;
    }

    public void setSenderCountry(String senderCountry) {
        this.senderCountry = senderCountry;
    }

    public Long getSenderCode() {
        return senderCode;
    }

    public void setSenderCode(Long senderCode) {
        this.senderCode = senderCode;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverStreetAddress() {
        return receiverStreetAddress;
    }

    public void setReceiverStreetAddress(String receiverStreetAddress) {
        this.receiverStreetAddress = receiverStreetAddress;
    }

    public String getReceiverCity() {
        return receiverCity;
    }

    public void setReceiverCity(String receiverCity) {
        this.receiverCity = receiverCity;
    }

    public String getReceiverState() {
        return receiverState;
    }

    public void setReceiverState(String receiverState) {
        this.receiverState = receiverState;
    }

    public String getReceiverCountry() {
        return receiverCountry;
    }

    public void setReceiverCountry(String receiverCountry) {
        this.receiverCountry = receiverCountry;
    }

    public Long getReceiverCode() {
        return receiverCode;
    }

    public void setReceiverCode(Long receiverCode) {
        this.receiverCode = receiverCode;
    }

    public Integer getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(Integer itemsCount) {
        this.itemsCount = itemsCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getDeclaredValue() {
        return declaredValue;
    }

    public void setDeclaredValue(BigDecimal declaredValue) {
        this.declaredValue = declaredValue;
    }

    public String getPickupInstructions() {
        return pickupInstructions;
    }

    public void setPickupInstructions(String pickupInstructions) {
        this.pickupInstructions = pickupInstructions;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public void setWidth(BigDecimal width) {
        this.width = width;
    }

    public BigDecimal getScale() {
        return scale;
    }

    public void setScale(BigDecimal scale) {
        this.scale = scale;
    }

    public BigDecimal getDimensionalWeight() {
        return dimensionalWeight;
    }

    public void setDimensionalWeight(BigDecimal dimensionalWeight) {
        this.dimensionalWeight = dimensionalWeight;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(String shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    public String getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(String shippingCost) {
        this.shippingCost = shippingCost;
    }

    public String getCostBreakdown() {
        return costBreakdown;
    }

    public void setCostBreakdown(String costBreakdown) {
        this.costBreakdown = costBreakdown;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalDateTime getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(LocalDateTime deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shipment shipment = (Shipment) o;
        return Objects.equals(trackingNumber, shipment.trackingNumber) &&
                Objects.equals(referenceNumber, shipment.referenceNumber) &&
                Objects.equals(customerSystemNumber, shipment.customerSystemNumber) &&
                Objects.equals(customerAccountNumber, shipment.customerAccountNumber) &&
                Objects.equals(customerName, shipment.customerName) &&
                Objects.equals(senderName, shipment.senderName) &&
                Objects.equals(senderPhone, shipment.senderPhone) &&
                Objects.equals(senderStreetAddress, shipment.senderStreetAddress) &&
                Objects.equals(senderCity, shipment.senderCity) &&
                Objects.equals(senderState, shipment.senderState) &&
                Objects.equals(senderCountry, shipment.senderCountry) &&
                Objects.equals(senderCode, shipment.senderCode) &&
                Objects.equals(receiverName, shipment.receiverName) &&
                Objects.equals(receiverEmail, shipment.receiverEmail) &&
                Objects.equals(receiverPhone, shipment.receiverPhone) &&
                Objects.equals(receiverStreetAddress, shipment.receiverStreetAddress) &&
                Objects.equals(receiverCity, shipment.receiverCity) &&
                Objects.equals(receiverState, shipment.receiverState) &&
                Objects.equals(receiverCountry, shipment.receiverCountry) &&
                Objects.equals(receiverCode, shipment.receiverCode) &&
                Objects.equals(itemsCount, shipment.itemsCount) &&
                Objects.equals(description, shipment.description) &&
                Objects.equals(quantity, shipment.quantity) &&
                Objects.equals(weight, shipment.weight) &&
                Objects.equals(declaredValue, shipment.declaredValue) &&
                Objects.equals(pickupInstructions, shipment.pickupInstructions) &&
                Objects.equals(height, shipment.height) &&
                Objects.equals(width, shipment.width) &&
                Objects.equals(scale, shipment.scale) &&
                Objects.equals(dimensionalWeight, shipment.dimensionalWeight) &&
                Objects.equals(type, shipment.type) &&
                Objects.equals(mode, shipment.mode) &&
                Objects.equals(shipmentStatus, shipment.shipmentStatus) &&
                Objects.equals(shippingCost, shipment.shippingCost) &&
                Objects.equals(costBreakdown, shipment.costBreakdown) &&
                Objects.equals(paymentStatus, shipment.paymentStatus) &&
                Objects.equals(bookingDate, shipment.bookingDate) &&
                Objects.equals(deliveryDate, shipment.deliveryDate);
    }

    @Override
    public int hashCode() {

        return Objects.hash(trackingNumber, referenceNumber, customerSystemNumber, customerAccountNumber, customerName, senderName, senderPhone, senderStreetAddress, senderCity, senderState, senderCountry, senderCode, receiverName, receiverEmail, receiverPhone, receiverStreetAddress, receiverCity, receiverState, receiverCountry, receiverCode, itemsCount, description, quantity, weight, declaredValue, pickupInstructions, height, width, scale, dimensionalWeight, type, mode, shipmentStatus, shippingCost, costBreakdown, paymentStatus, bookingDate, deliveryDate);
    }
}
