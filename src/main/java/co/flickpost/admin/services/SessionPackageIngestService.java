package co.flickpost.admin.services;

import co.flickpost.admin.models.PackageReference;
import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.FpReferencePayload;
import co.flickpost.admin.models.json.FpSessionPackageIngestRequest;
import co.flickpost.admin.models.json.FpSessionPackageIngestResponse;
import co.flickpost.admin.models.json.PendingDuplicateSessionPackage;
import co.flickpost.admin.repositories.PackageReferenceDao;
import co.flickpost.admin.repositories.SessionPackageDao;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class SessionPackageIngestService {

    private static final Logger logger = LogManager.getLogger(SessionPackageIngestService.class);
    private static final String ON_HOLD = "ON HOLD";
    private static final String PROCEED = "PROCEED";
    private static final String PENDING = "UNKNOWN";
    private static final String REFERENCE_SOURCE_API = "API";
    private static final String REFERENCE_SOURCE_DB = "DB";

    @Autowired
    private SessionPackageDao sessionPackageDao;

    @Autowired
    private PackageReferenceDao packageReferenceDao;

    @Autowired
    private ScanSessionSseService scanSessionSseService;

    @Autowired
    private PendingDuplicateSessionPackageService pendingDuplicateSessionPackageService;

    @Autowired
    private ScanSessionService scanSessionService;

    @Transactional
    public FpSessionPackageIngestResponse ingest(FpSessionPackageIngestRequest request) {
        validate(request);

        SessionPackage existingSessionPackage = sessionPackageDao.findSessionPackageByTrackingNumber(request.getTrackingNumber());
        SessionPackage sessionPackage = new SessionPackage();
        if (existingSessionPackage != null) {
            sessionPackage.setId(existingSessionPackage.getId());
        }
        sessionPackage.setTrackingNumber(request.getTrackingNumber());

        sessionPackage.setHub(request.getHub());
        sessionPackage.setAuditedLength(request.getAuditedLength());
        sessionPackage.setAuditedWidth(request.getAuditedWidth());
        sessionPackage.setAuditedHeight(request.getAuditedHeight());
        sessionPackage.setAuditedActualWeight(request.getAuditedActualWeight());
        sessionPackage.setAuditedVolumetricWeight(request.getAuditedVolumetricWeight());
        sessionPackage.setAuditedChargeableWeight(request.getAuditedChargeableWeight());
        sessionPackage.setHid(request.getHid());
        sessionPackage.setDateTime(request.getDateTime());
        sessionPackage.setSessionId(request.getSessionId());
        sessionPackage.setImageInfos(request.getImageInfos());

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
                sessionPackage.getDeclaredActualWeight(),
                sessionPackage.getAuditedActualWeight(),
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

        if (existingSessionPackage != null) {
            SessionPackage oldRecord = cloneForDuplicate(existingSessionPackage);
            SessionPackage newRecord = cloneForDuplicate(sessionPackage);
            newRecord.setId(null);
            pendingDuplicateSessionPackageService.put(new PendingDuplicateSessionPackage(sessionPackage.getTrackingNumber(), oldRecord, newRecord));
            publishDuplicateDetectedAfterCommit(sessionPackage.getTrackingNumber());
            return new FpSessionPackageIngestResponse(
                    true,
                    sessionPackage.getTrackingNumber(),
                    finalStatus,
                    dataSource,
                    isUnderdeclared,
                    "Duplicate record detected. Awaiting user resolution"
            );
        }

        sessionPackageDao.insert(sessionPackage);
        scanSessionService.incrementActiveSessionTotalPackages();
        publishPackageIngestedAfterCommit(sessionPackage.getTrackingNumber());

        return new FpSessionPackageIngestResponse(
                true,
                sessionPackage.getTrackingNumber(),
                finalStatus,
                dataSource,
                isUnderdeclared,
                "SessionPackage ingested successfully"
        );
    }

    private void publishPackageIngestedAfterCommit(String trackingNumber) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scanSessionSseService.publishPackageIngested(trackingNumber);
                }
            });
        } else {
            logger.warn("Transaction synchronization was not active while publishing package-ingested for {}. Publishing immediately.", trackingNumber);
            scanSessionSseService.publishPackageIngested(trackingNumber);
        }
    }

    private void publishDuplicateDetectedAfterCommit(String trackingNumber) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scanSessionSseService.publishDuplicateDetected(trackingNumber);
                }
            });
        } else {
            logger.warn("Transaction synchronization was not active while publishing duplicate-detected for {}. Publishing immediately.", trackingNumber);
            scanSessionSseService.publishDuplicateDetected(trackingNumber);
        }
    }

    private SessionPackage cloneForDuplicate(SessionPackage source) {
        SessionPackage copy = new SessionPackage();
        copy.setId(source.getId());
        copy.setTrackingNumber(source.getTrackingNumber());
        copy.setHub(source.getHub());
        copy.setShipmentId(source.getShipmentId());
        copy.setStatus(source.getStatus());
        copy.setAuditedLength(source.getAuditedLength());
        copy.setAuditedWidth(source.getAuditedWidth());
        copy.setAuditedHeight(source.getAuditedHeight());
        copy.setAuditedActualWeight(source.getAuditedActualWeight());
        copy.setDeclaredLength(source.getDeclaredLength());
        copy.setDeclaredWidth(source.getDeclaredWidth());
        copy.setDeclaredHeight(source.getDeclaredHeight());
        copy.setDeclaredActualWeight(source.getDeclaredActualWeight());
        copy.setDeclaredChargeableWeight(source.getDeclaredChargeableWeight());
        copy.setDeclaredVolumetricWeight(source.getDeclaredVolumetricWeight());
        copy.setClientPaidWeight(source.getClientPaidWeight());
        copy.setDestinationCountry(source.getDestinationCountry());
        copy.setServiceProviderName(source.getServiceProviderName());
        copy.setContainsLiquid(source.getContainsLiquid());
        copy.setContainsBattery(source.getContainsBattery());
        copy.setItemCondition(source.getItemCondition());
        copy.setIsCommercialPackaging(source.getIsCommercialPackaging());
        copy.setShippingMode(source.getShippingMode());
        copy.setAuditedVolumetricWeight(source.getAuditedVolumetricWeight());
        copy.setAuditedChargeableWeight(source.getAuditedChargeableWeight());
        copy.setHid(source.getHid());
        copy.setDateTime(source.getDateTime());
        copy.setSessionId(source.getSessionId());
        copy.setImageInfos(source.getImageInfos());
        copy.setReferenceSource(source.getReferenceSource());
        copy.setIsUnderdeclared(source.getIsUnderdeclared());
        return copy;
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
        packageReference.setDeclaredActualWeight(referencePayload.getDeclaredActualWeight());
        packageReference.setDeclaredChargeableWeight(referencePayload.getDeclaredChargeableWeight());
        packageReference.setDeclaredVolumetricWeight(referencePayload.getDeclaredVolumetricWeight());
        packageReference.setDeclaredLength(referencePayload.getDeclaredLength());
        packageReference.setDeclaredWidth(referencePayload.getDeclaredWidth());
        packageReference.setDeclaredHeight(referencePayload.getDeclaredHeight());
        packageReference.setClientPaidWeight(referencePayload.getClientPaidWeight());
        packageReference.setDestinationCountry(referencePayload.getDestinationCountry());
        packageReference.setItemCondition(referencePayload.getItemCondition());
        packageReference.setContainsLiquid(referencePayload.getContainsLiquid());
        packageReference.setContainsBattery(referencePayload.getContainsBattery());
        packageReference.setIsCommercialPackaging(referencePayload.getIsCommercialPackaging());
        packageReference.setShippingMode(referencePayload.getShippingMode());
        packageReference.setServiceProviderName(referencePayload.getServiceProviderName());
        packageReference.setTtlTimestamp(LocalDateTime.now());

        packageReferenceDao.save(packageReference);
    }

    private void applyReferencePayload(SessionPackage sessionPackage, FpReferencePayload referencePayload) {
        sessionPackage.setShipmentId(referencePayload.getShipmentId());
        sessionPackage.setDeclaredActualWeight(referencePayload.getDeclaredActualWeight());
        sessionPackage.setDeclaredChargeableWeight(referencePayload.getDeclaredChargeableWeight());
        sessionPackage.setDeclaredVolumetricWeight(referencePayload.getDeclaredVolumetricWeight());
        sessionPackage.setDeclaredLength(referencePayload.getDeclaredLength());
        sessionPackage.setDeclaredWidth(referencePayload.getDeclaredWidth());
        sessionPackage.setDeclaredHeight(referencePayload.getDeclaredHeight());
        sessionPackage.setClientPaidWeight(referencePayload.getClientPaidWeight());
        sessionPackage.setDestinationCountry(referencePayload.getDestinationCountry());
        sessionPackage.setItemCondition(referencePayload.getItemCondition());
        sessionPackage.setContainsLiquid(referencePayload.getContainsLiquid());
        sessionPackage.setContainsBattery(referencePayload.getContainsBattery());
        sessionPackage.setIsCommercialPackaging(referencePayload.getIsCommercialPackaging());
        sessionPackage.setShippingMode(referencePayload.getShippingMode());
        sessionPackage.setServiceProviderName(referencePayload.getServiceProviderName());
    }

    private void applyPackageReference(SessionPackage sessionPackage, PackageReference packageReference) {
        sessionPackage.setShipmentId(packageReference.getShipmentId());
        sessionPackage.setDeclaredActualWeight(packageReference.getDeclaredActualWeight());
        sessionPackage.setDeclaredChargeableWeight(packageReference.getDeclaredChargeableWeight());
        sessionPackage.setDeclaredVolumetricWeight(packageReference.getDeclaredVolumetricWeight());
        sessionPackage.setDeclaredLength(packageReference.getDeclaredLength());
        sessionPackage.setDeclaredWidth(packageReference.getDeclaredWidth());
        sessionPackage.setDeclaredHeight(packageReference.getDeclaredHeight());
        sessionPackage.setClientPaidWeight(packageReference.getClientPaidWeight());
        sessionPackage.setDestinationCountry(packageReference.getDestinationCountry());
        sessionPackage.setItemCondition(packageReference.getItemCondition());
        sessionPackage.setContainsLiquid(packageReference.getContainsLiquid());
        sessionPackage.setContainsBattery(packageReference.getContainsBattery());
        sessionPackage.setIsCommercialPackaging(packageReference.getIsCommercialPackaging());
        sessionPackage.setShippingMode(packageReference.getShippingMode());
        sessionPackage.setServiceProviderName(packageReference.getServiceProviderName());
    }

    private Boolean evaluateIsUnderdeclared(String trackingNumber,
                                            String shippingMode,
                                            BigDecimal declaredWeight,
                                            BigDecimal auditedActualWeight,
                                            BigDecimal auditedVolumetricWeight) {
        if (declaredWeight == null) {
            logger.info("{} - isUnderdeclared could not be evaluated because declaredWeight is null", trackingNumber);
            return null;
        }
        if (auditedActualWeight == null) {
            logger.info("{} - isUnderdeclared could not be evaluated because auditedActualWeight is null", trackingNumber);
            return null;
        }
        if (StringUtils.isBlank(shippingMode)) {
            logger.info("{} - isUnderdeclared could not be evaluated because shippingMode is blank", trackingNumber);
            return null;
        }

        double declared = declaredWeight.doubleValue();
        double audited = auditedActualWeight.doubleValue();
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
