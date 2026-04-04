package co.flickpost.admin.services;

import co.flickpost.admin.models.PackageReference;
import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.FpReferencePayload;
import co.flickpost.admin.models.json.FpSessionPackageIngestRequest;
import co.flickpost.admin.models.json.FpSessionPackageIngestResponse;
import co.flickpost.admin.repositories.PackageReferenceDao;
import co.flickpost.admin.repositories.SessionPackageDao;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;

@Service
public class SessionPackageIngestService {

    private static final Logger logger = LogManager.getLogger(SessionPackageIngestService.class);
    private static final String ON_HOLD = "ON_HOLD";
    private static final String PROCEED = "PROCEED";
    private static final String PENDING = "PENDING";
    private static final String REFERENCE_SOURCE_API = "API";
    private static final String REFERENCE_SOURCE_DB = "DB";

    @Autowired
    private SessionPackageDao sessionPackageDao;

    @Autowired
    private PackageReferenceDao packageReferenceDao;

    @Transactional
    public FpSessionPackageIngestResponse ingest(FpSessionPackageIngestRequest request) {
        validate(request);

        SessionPackage sessionPackage = sessionPackageDao.findSessionPackageByTrackingNumber(request.getTrackingNumber());
        if (sessionPackage == null) {
            sessionPackage = new SessionPackage();
            sessionPackage.setTrackingNumber(request.getTrackingNumber());
        }

        sessionPackage.setHub(request.getHub());
        sessionPackage.setAuditedLength(request.getAuditedLength());
        sessionPackage.setAuditedWidth(request.getAuditedWidth());
        sessionPackage.setAuditedHeight(request.getAuditedHeight());
        sessionPackage.setAuditedWeight(request.getAuditedWeight());
        sessionPackage.setAuditedVolumetricWeight(request.getAuditedVolumetricWeight());
        sessionPackage.setChargeableWeight(request.getChargeableWeight());
        sessionPackage.setHid(request.getHid());
        sessionPackage.setDateTime(request.getDateTime());
        sessionPackage.setSessionId(request.getSessionId());

        String dataSource = null;

        if (Boolean.TRUE.equals(request.getIsFilled()) && request.getReference() != null) {
            upsertReferenceFromPayload(request.getTrackingNumber(), request.getReference());
            applyReferencePayload(sessionPackage, request.getReference());
            sessionPackage.setReferenceSource(REFERENCE_SOURCE_API);
            dataSource = REFERENCE_SOURCE_API;
        } else {
            PackageReference packageReference = packageReferenceDao.findByTrackingNumber(request.getTrackingNumber());
            if (packageReference != null) {
                applyPackageReference(sessionPackage, packageReference);
                sessionPackage.setReferenceSource(REFERENCE_SOURCE_DB);
                dataSource = REFERENCE_SOURCE_DB;
            }
        }

        Boolean isUnderdeclared = evaluateIsUnderdeclared(
                request.getTrackingNumber(),
                sessionPackage.getShippingMode(),
                sessionPackage.getDeclaredWeight(),
                sessionPackage.getAuditedWeight(),
                sessionPackage.getAuditedVolumetricWeight()
        );
        String finalStatus = evaluateFinalStatus(
                sessionPackage.getContainsLiquid(),
                sessionPackage.getContainsBattery(),
                sessionPackage.getItemCondition(),
                sessionPackage.getIsCommercialPackaging(),
                isUnderdeclared,
                sessionPackage.getReferenceSource()
        );

        sessionPackage.setIsUnderdeclared(isUnderdeclared);
        sessionPackage.setStatus(finalStatus);

        if (sessionPackage.getId() == null) {
            sessionPackageDao.insert(sessionPackage);
        } else {
            sessionPackageDao.singleUpdate(sessionPackage);
        }

        return new FpSessionPackageIngestResponse(
                true,
                sessionPackage.getTrackingNumber(),
                finalStatus,
                dataSource,
                isUnderdeclared,
                "SessionPackage ingested successfully"
        );
    }

    private void validate(FpSessionPackageIngestRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getIsFilled() == null) {
            throw new IllegalArgumentException("isFilled is required");
        }
        if (StringUtils.isBlank(request.getTrackingNumber())) {
            throw new IllegalArgumentException("trackingNumber is required");
        }
    }

    private void upsertReferenceFromPayload(String trackingNumber, FpReferencePayload referencePayload) {
        PackageReference packageReference = packageReferenceDao.findByTrackingNumber(trackingNumber);
        if (packageReference == null) {
            packageReference = new PackageReference();
            packageReference.setTrackingNumber(trackingNumber);
        }

        packageReference.setShipmentId(referencePayload.getShipmentId());
        packageReference.setDeclaredWeight(referencePayload.getDeclaredWeight());
        packageReference.setDeclaredLength(referencePayload.getDeclaredLength());
        packageReference.setDeclaredWidth(referencePayload.getDeclaredWidth());
        packageReference.setDeclaredHeight(referencePayload.getDeclaredHeight());
        packageReference.setClientPaidHeight(referencePayload.getClientPaidHeight());
        packageReference.setItemCondition(referencePayload.getItemCondition());
        packageReference.setContainsLiquid(referencePayload.getContainsLiquid());
        packageReference.setContainsBattery(referencePayload.getContainsBattery());
        packageReference.setIsCommercialPackaging(referencePayload.getIsCommercialPackaging());
        packageReference.setShippingMode(referencePayload.getShippingMode());

        packageReferenceDao.save(packageReference);
    }

    private void applyReferencePayload(SessionPackage sessionPackage, FpReferencePayload referencePayload) {
        sessionPackage.setShipmentId(referencePayload.getShipmentId());
        sessionPackage.setDeclaredWeight(referencePayload.getDeclaredWeight());
        sessionPackage.setDeclaredLength(referencePayload.getDeclaredLength());
        sessionPackage.setDeclaredWidth(referencePayload.getDeclaredWidth());
        sessionPackage.setDeclaredHeight(referencePayload.getDeclaredHeight());
        sessionPackage.setClientPaidHeight(referencePayload.getClientPaidHeight());
        sessionPackage.setItemCondition(referencePayload.getItemCondition());
        sessionPackage.setContainsLiquid(referencePayload.getContainsLiquid());
        sessionPackage.setContainsBattery(referencePayload.getContainsBattery());
        sessionPackage.setIsCommercialPackaging(referencePayload.getIsCommercialPackaging());
        sessionPackage.setShippingMode(referencePayload.getShippingMode());
    }

    private void applyPackageReference(SessionPackage sessionPackage, PackageReference packageReference) {
        sessionPackage.setShipmentId(packageReference.getShipmentId());
        sessionPackage.setDeclaredWeight(packageReference.getDeclaredWeight());
        sessionPackage.setDeclaredLength(packageReference.getDeclaredLength());
        sessionPackage.setDeclaredWidth(packageReference.getDeclaredWidth());
        sessionPackage.setDeclaredHeight(packageReference.getDeclaredHeight());
        sessionPackage.setClientPaidHeight(packageReference.getClientPaidHeight());
        sessionPackage.setItemCondition(packageReference.getItemCondition());
        sessionPackage.setContainsLiquid(packageReference.getContainsLiquid());
        sessionPackage.setContainsBattery(packageReference.getContainsBattery());
        sessionPackage.setIsCommercialPackaging(packageReference.getIsCommercialPackaging());
        sessionPackage.setShippingMode(packageReference.getShippingMode());
    }

    private Boolean evaluateIsUnderdeclared(String trackingNumber,
                                            String shippingMode,
                                            BigDecimal declaredWeight,
                                            BigDecimal auditedWeight,
                                            BigDecimal auditedVolumetricWeight) {
        if (declaredWeight == null) {
            logger.info("{} - isUnderdeclared could not be evaluated because declaredWeight is null", trackingNumber);
            return null;
        }
        if (auditedWeight == null) {
            logger.info("{} - isUnderdeclared could not be evaluated because auditedWeight is null", trackingNumber);
            return null;
        }
        if (StringUtils.isBlank(shippingMode)) {
            logger.info("{} - isUnderdeclared could not be evaluated because shippingMode is blank", trackingNumber);
            return null;
        }

        double declared = declaredWeight.doubleValue();
        double audited = auditedWeight.doubleValue();
        double volumetric = auditedVolumetricWeight != null ? auditedVolumetricWeight.doubleValue() : 0D;
        double evaluationWeight;

        if ("Economy".equalsIgnoreCase(shippingMode)) {
            evaluationWeight = audited;
        } else if ("Standard".equalsIgnoreCase(shippingMode)
                || "Priority".equalsIgnoreCase(shippingMode)
                || "Greenlane".equalsIgnoreCase(shippingMode)) {
            evaluationWeight = Math.max(audited, volumetric);
        } else {
            logger.info("{} - isUnderdeclared could not be evaluated because shippingMode {} is unsupported", trackingNumber, shippingMode);
            return null;
        }

        return declared < evaluationWeight;
    }

    private String evaluateFinalStatus(Boolean containsLiquid,
                                       Boolean containsBattery,
                                       String itemCondition,
                                       Boolean isCommercialPackaging,
                                       Boolean isUnderdeclared,
                                       String referenceSource) {
        if (referenceSource == null || isUnderdeclared == null) {
            return PENDING;
        }

        if (Boolean.TRUE.equals(containsLiquid)
                || Boolean.TRUE.equals(containsBattery)
                || isUsed(itemCondition)
                || Boolean.FALSE.equals(isCommercialPackaging)
                || Boolean.TRUE.equals(isUnderdeclared)) {
            return ON_HOLD;
        }

        return PROCEED;
    }

    private boolean isUsed(String itemCondition) {
        return StringUtils.isNotBlank(itemCondition) && !"new".equalsIgnoreCase(itemCondition.trim());
    }
}
