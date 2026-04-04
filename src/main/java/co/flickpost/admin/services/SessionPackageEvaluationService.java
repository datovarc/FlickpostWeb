package co.flickpost.admin.services;

import co.flickpost.admin.models.PackageReference;
import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.SessionPackageEvaluationRequest;
import co.flickpost.admin.models.json.SessionPackageEvaluationResponse;
import co.flickpost.admin.repositories.PackageReferenceDao;
import co.flickpost.admin.repositories.SessionPackageDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Locale;

@Service
public class SessionPackageEvaluationService {

    private static final String ON_HOLD = "ON_HOLD";
    private static final String PROCEED = "PROCEED";
    private static final String PENDING = "PENDING";
    private static final String REFERENCE_SOURCE_API = "api";
    private static final String REFERENCE_SOURCE_DB = "DB";

    @Autowired
    private SessionPackageDao sessionPackageDao;

    @Autowired
    private PackageReferenceDao packageReferenceDao;

    @Transactional
    public SessionPackageEvaluationResponse evaluate(SessionPackageEvaluationRequest request) {
        validate(request);

        String trackingNumber = readString(request, "getTrackingNumber");
        SessionPackage sessionPackage = sessionPackageDao.findSessionPackageByTrackingNumber(trackingNumber);
        if (sessionPackage == null) {
            throw new IllegalArgumentException("SessionPackage not found for trackingNumber: " + trackingNumber);
        }

        EvaluationData evaluationData = buildEvaluationData(request, sessionPackage);
        String finalStatus = determineFinalStatus(evaluationData);

        applyResolvedData(sessionPackage, evaluationData);
        write(sessionPackage, "setStatus", String.class, finalStatus);
        sessionPackageDao.singleUpdate(sessionPackage);

        return new SessionPackageEvaluationResponse(
                true,
                readString(sessionPackage, "getTrackingNumber"),
                finalStatus,
                evaluationData.dataSource,
                evaluationData.backfilledFromReference,
                "SessionPackage evaluated successfully"
        );
    }

    private void validate(SessionPackageEvaluationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (readBooleanObject(request, "getIsFilled") == null) {
            throw new IllegalArgumentException("isFilled is required");
        }
        if (StringUtils.isBlank(readString(request, "getTrackingNumber"))) {
            throw new IllegalArgumentException("trackingNumber is required");
        }
    }

    private EvaluationData buildEvaluationData(SessionPackageEvaluationRequest request, SessionPackage sessionPackage) {
        EvaluationData data = new EvaluationData();
        data.trackingNumber = readString(request, "getTrackingNumber");

        if (Boolean.TRUE.equals(readBooleanObject(request, "getIsFilled"))) {
            data.shippingMode = firstNonBlank(readString(request, "getShippingMode"), readString(sessionPackage, "getShippingMode"));
            data.containsLiquid = firstNonNull(readBooleanObject(request, "getContainsLiquid"), readBooleanObject(sessionPackage, "getContainsLiquid"));
            data.containsBattery = firstNonNull(readBooleanObject(request, "getContainsBattery"), readBooleanObject(sessionPackage, "getContainsBattery"));
            data.itemCondition = firstNonBlank(readString(request, "getItemCondition"), readString(sessionPackage, "getItemCondition"));
            data.isCommercialPackaging = firstNonNull(readBooleanObject(request, "getIsCommercialPackaging"), readBooleanObject(sessionPackage, "getIsCommercialPackaging"));
            data.declaredWeight = firstNonNull(readBigDecimal(request, "getDeclaredWeight"), readBigDecimal(sessionPackage, "getDeclaredWeight"));
            data.auditedWeight = firstNonNull(readBigDecimal(request, "getAuditedWeight"), readBigDecimal(sessionPackage, "getAuditedWeight"));
            data.auditedVolumetricWeight = firstNonNull(readBigDecimal(request, "getAuditedVolumetricWeight"), readBigDecimal(sessionPackage, "getAuditedVolumetricWeight"));
            data.declaredLength = firstNonNull(readBigDecimal(request, "getDeclaredLength"), readBigDecimal(sessionPackage, "getDeclaredLength"));
            data.declaredWidth = firstNonNull(readBigDecimal(request, "getDeclaredWidth"), readBigDecimal(sessionPackage, "getDeclaredWidth"));
            data.declaredHeight = firstNonNull(readBigDecimal(request, "getDeclaredHeight"), readBigDecimal(sessionPackage, "getDeclaredHeight"));
            data.clientPaidHeight = firstNonNull(readBigDecimal(request, "getClientPaidHeight"), readBigDecimal(sessionPackage, "getClientPaidHeight"));
            data.destinationCountry = firstNonBlank(readString(request, "getDestinationCountry"), readString(sessionPackage, "getDestinationCountry"));
            data.shipmentId = firstNonBlank(readString(request, "getShipmentId"), readString(sessionPackage, "getShipmentId"));
            data.dataSource = REFERENCE_SOURCE_API;
            data.backfilledFromReference = false;
            data.filledFromPayload = true;
            data.referenceFound = false;
            return data;
        }

        PackageReference packageReference = packageReferenceDao.findByTrackingNumber(data.trackingNumber);
        if (packageReference == null) {
            data.shippingMode = readString(sessionPackage, "getShippingMode");
            data.containsLiquid = readBooleanObject(sessionPackage, "getContainsLiquid");
            data.containsBattery = readBooleanObject(sessionPackage, "getContainsBattery");
            data.itemCondition = readString(sessionPackage, "getItemCondition");
            data.isCommercialPackaging = readBooleanObject(sessionPackage, "getIsCommercialPackaging");
            data.declaredWeight = readBigDecimal(sessionPackage, "getDeclaredWeight");
            data.auditedWeight = readBigDecimal(sessionPackage, "getAuditedWeight");
            data.auditedVolumetricWeight = readBigDecimal(sessionPackage, "getAuditedVolumetricWeight");
            data.declaredLength = readBigDecimal(sessionPackage, "getDeclaredLength");
            data.declaredWidth = readBigDecimal(sessionPackage, "getDeclaredWidth");
            data.declaredHeight = readBigDecimal(sessionPackage, "getDeclaredHeight");
            data.clientPaidHeight = readBigDecimal(sessionPackage, "getClientPaidHeight");
            data.destinationCountry = readString(sessionPackage, "getDestinationCountry");
            data.shipmentId = readString(sessionPackage, "getShipmentId");
            data.dataSource = readString(sessionPackage, "getReferenceSource");
            data.backfilledFromReference = false;
            data.referenceFound = false;
            return data;
        }

        data.shippingMode = firstNonBlank(readString(packageReference, "getShippingMode"), readString(sessionPackage, "getShippingMode"));
        data.containsLiquid = firstNonNull(readBooleanObject(packageReference, "getContainsLiquid"), readBooleanObject(sessionPackage, "getContainsLiquid"));
        data.containsBattery = firstNonNull(readBooleanObject(packageReference, "getContainsBattery"), readBooleanObject(sessionPackage, "getContainsBattery"));
        data.itemCondition = firstNonBlank(readString(packageReference, "getItemCondition"), readString(sessionPackage, "getItemCondition"));
        data.isCommercialPackaging = firstNonNull(readBooleanObject(packageReference, "getIsCommercialPackaging"), readBooleanObject(sessionPackage, "getIsCommercialPackaging"));
        data.declaredWeight = firstNonNull(readBigDecimal(packageReference, "getDeclaredWeight"), readBigDecimal(sessionPackage, "getDeclaredWeight"));
        data.auditedWeight = readBigDecimal(sessionPackage, "getAuditedWeight");
        data.auditedVolumetricWeight = readBigDecimal(sessionPackage, "getAuditedVolumetricWeight");
        data.declaredLength = firstNonNull(readBigDecimal(packageReference, "getDeclaredLength"), readBigDecimal(sessionPackage, "getDeclaredLength"));
        data.declaredWidth = firstNonNull(readBigDecimal(packageReference, "getDeclaredWidth"), readBigDecimal(sessionPackage, "getDeclaredWidth"));
        data.declaredHeight = firstNonNull(readBigDecimal(packageReference, "getDeclaredHeight"), readBigDecimal(sessionPackage, "getDeclaredHeight"));
        data.clientPaidHeight = firstNonNull(readBigDecimal(packageReference, "getClientPaidHeight"), readBigDecimal(sessionPackage, "getClientPaidHeight"));
        data.destinationCountry = firstNonBlank(readString(packageReference, "getDestinationCountry"), readString(sessionPackage, "getDestinationCountry"));
        data.shipmentId = firstNonBlank(readString(packageReference, "getShipmentId"), readString(sessionPackage, "getShipmentId"));
        data.dataSource = REFERENCE_SOURCE_DB;
        data.backfilledFromReference = true;
        data.referenceFound = true;
        return data;
    }

    private String determineFinalStatus(EvaluationData data) {
        if (!data.filledFromPayload && !data.referenceFound) {
            return PENDING;
        }

        boolean anyLiquid = Boolean.TRUE.equals(data.containsLiquid);
        boolean anyBattery = Boolean.TRUE.equals(data.containsBattery);
        boolean isUsed = isUsedCondition(data.itemCondition);
        boolean noCommercialPackaging = Boolean.FALSE.equals(data.isCommercialPackaging);

        String status = (anyLiquid || anyBattery || isUsed || noCommercialPackaging) ? ON_HOLD : PROCEED;

        if (isUnderDeclared(data)) {
            return ON_HOLD;
        }

        return status;
    }

    private boolean isUnderDeclared(EvaluationData data) {
        if (data.declaredWeight == null || data.auditedWeight == null || StringUtils.isBlank(data.shippingMode)) {
            return false;
        }

        BigDecimal leftSide;
        String normalizedMode = data.shippingMode.trim().toLowerCase(Locale.ROOT);
        switch (normalizedMode) {
            case "economy":
                leftSide = data.auditedWeight;
                break;
            case "standard":
            case "priority":
            case "greenlane":
                leftSide = max(data.auditedWeight, data.auditedVolumetricWeight);
                break;
            default:
                return false;
        }

        if (leftSide == null) {
            return false;
        }

        return data.declaredWeight.compareTo(leftSide) < 0;
    }

    private void applyResolvedData(SessionPackage sessionPackage, EvaluationData data) {
        write(sessionPackage, "setShippingMode", String.class, data.shippingMode);
        write(sessionPackage, "setContainsLiquid", Boolean.class, data.containsLiquid);
        write(sessionPackage, "setContainsBattery", Boolean.class, data.containsBattery);
        write(sessionPackage, "setItemCondition", String.class, data.itemCondition);
        write(sessionPackage, "setIsCommercialPackaging", Boolean.class, data.isCommercialPackaging);
        write(sessionPackage, "setDeclaredWeight", BigDecimal.class, data.declaredWeight);
        write(sessionPackage, "setDeclaredLength", BigDecimal.class, data.declaredLength);
        write(sessionPackage, "setDeclaredWidth", BigDecimal.class, data.declaredWidth);
        write(sessionPackage, "setDeclaredHeight", BigDecimal.class, data.declaredHeight);
        write(sessionPackage, "setClientPaidHeight", BigDecimal.class, data.clientPaidHeight);
        write(sessionPackage, "setDestinationCountry", String.class, data.destinationCountry);
        write(sessionPackage, "setShipmentId", String.class, data.shipmentId);
        write(sessionPackage, "setReferenceSource", String.class, data.dataSource);
    }

    private boolean isUsedCondition(String itemCondition) {
        return StringUtils.isNotBlank(itemCondition)
                && !"new".equalsIgnoreCase(itemCondition.trim());
    }

    private BigDecimal max(BigDecimal first, BigDecimal second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.max(second);
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private String readString(Object target, String methodName) {
        Object value = invoke(target, methodName);
        return value != null ? String.valueOf(value) : null;
    }

    private Boolean readBooleanObject(Object target, String methodName) {
        Object value = invoke(target, methodName);
        return value instanceof Boolean ? (Boolean) value : null;
    }

    private BigDecimal readBigDecimal(Object target, String methodName) {
        Object value = invoke(target, methodName);
        return value instanceof BigDecimal ? (BigDecimal) value : null;
    }

    private Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke method " + methodName + " on " + target.getClass().getSimpleName(), e);
        }
    }

    private <T> void write(Object target, String methodName, Class<T> parameterType, T value) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterType);
            method.invoke(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke method " + methodName + " on " + target.getClass().getSimpleName(), e);
        }
    }

    private static class EvaluationData {
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
        private String dataSource;
        private boolean backfilledFromReference;
        private boolean referenceFound;
        private boolean filledFromPayload;
    }
}
