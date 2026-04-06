package co.flickpost.admin.services;

import co.flickpost.admin.models.Package;
import co.flickpost.admin.models.PackageReference;
import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.FpReferencePayload;
import co.flickpost.admin.models.json.RecheckPendingSummaryResponse;
import co.flickpost.admin.repositories.PackageDao;
import co.flickpost.admin.repositories.PackageReferenceDao;
import co.flickpost.admin.repositories.SessionPackageDao;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PackageEvaluationService {

    private static final Logger logger = LogManager.getLogger(PackageEvaluationService.class);
    private static final String ON_HOLD = "ON HOLD";
    private static final String PROCEED = "PROCEED";
    private static final String PENDING = "UNKNOWN";
    private static final String REFERENCE_SOURCE_API = "API";
    private static final String REFERENCE_SOURCE_DB = "DB";
    private static final String CONTEXT_WEIGHT = "weight";
    private static final String CONTEXT_SCAN_SESSION = "scan-session";

    @Autowired
    private PackageDao packageDao;

    @Autowired
    private SessionPackageDao sessionPackageDao;

    @Autowired
    private PackageReferenceDao packageReferenceDao;

    @Autowired
    private ThirdPartyParcelDetailsService thirdPartyParcelDetailsService;

    @Autowired
    private ScanSessionSseService scanSessionSseService;

    @Transactional
    public RecheckPendingSummaryResponse recheckPending(String context, List<String> requestedTrackingNumbers) {
        String normalizedContext = normalizeContext(context);
        if (normalizedContext == null) {
            throw new IllegalArgumentException("Unsupported context: " + context);
        }

        Set<String> requestedTrackingSet = new LinkedHashSet<>();
        if (requestedTrackingNumbers != null && !requestedTrackingNumbers.isEmpty()) {
            for (String trackingNumber : requestedTrackingNumbers) {
                if (StringUtils.isNotBlank(trackingNumber)) {
                    requestedTrackingSet.add(trackingNumber.trim());
                }
            }
        }

        boolean usedSelectedRows = !requestedTrackingSet.isEmpty();

        List<?> pendingRecords = requestedTrackingSet.isEmpty()
                ? loadAllPending(normalizedContext)
                : loadPendingByTrackingNumbers(normalizedContext, new ArrayList<>(requestedTrackingSet));

        if (pendingRecords == null || pendingRecords.isEmpty()) {
            return new RecheckPendingSummaryResponse(
                    true,
                    normalizedContext,
                    0,
                    0,
                    0,
                    0,
                    usedSelectedRows
                            ? "Re-checking done, there are no UNKNOWN packages selected"
                            : "Re-checking done, there are no UNKNOWN packages remaining",
                    usedSelectedRows
            );
        }

        List<String> pendingTrackingNumbers = new ArrayList<>();
        for (Object record : pendingRecords) {
            if (record instanceof Package) {
                pendingTrackingNumbers.add(((Package) record).getTrackingNumber());
            } else if (record instanceof SessionPackage) {
                pendingTrackingNumbers.add(((SessionPackage) record).getTrackingNumber());
            }
        }

        Map<String, PackageReference> apiReferences = pendingTrackingNumbers.isEmpty()
                ? new LinkedHashMap<>()
                : thirdPartyParcelDetailsService.fetchAndUpsert(pendingTrackingNumbers);

        int recheckedCount = 0;
        int foundFromApiCount = 0;
        int foundFromDbCount = 0;
        int stillMissingCount = 0;

        for (Object record : pendingRecords) {
            if (record == null) {
                continue;
            }
            recheckedCount++;
            RecheckOutcome outcome = CONTEXT_WEIGHT.equals(normalizedContext)
                    ? recheckPackage((Package) record, apiReferences)
                    : recheckSessionPackage((SessionPackage) record, apiReferences);

            if (outcome == RecheckOutcome.API) {
                foundFromApiCount++;
            } else if (outcome == RecheckOutcome.DB) {
                foundFromDbCount++;
            } else {
                stillMissingCount++;
            }
        }

        return new RecheckPendingSummaryResponse(
                true,
                normalizedContext,
                recheckedCount,
                foundFromApiCount,
                foundFromDbCount,
                stillMissingCount,
                "Re-check completed",
                usedSelectedRows
        );
    }

    private List<?> loadAllPending(String context) {
        return CONTEXT_WEIGHT.equals(context) ? packageDao.findAllPending() : sessionPackageDao.findAllPending();
    }

    private List<?> loadPendingByTrackingNumbers(String context, List<String> trackingNumbers) {
        return CONTEXT_WEIGHT.equals(context)
                ? packageDao.findPendingByTrackingNumbers(trackingNumbers)
                : sessionPackageDao.findPendingByTrackingNumbers(trackingNumbers);
    }

    private RecheckOutcome recheckPackage(Package pkg, Map<String, PackageReference> apiReferences) {
        String trackingNumber = pkg.getTrackingNumber();
        PackageReference apiReference = apiReferences.get(trackingNumber);
        if (apiReference != null) {
            applyPackageReference(pkg, apiReference);
            pkg.setReferenceSource(REFERENCE_SOURCE_API);
            finalizePackage(pkg);
            packageDao.singleUpdate(pkg);
            return RecheckOutcome.API;
        }

        PackageReference dbReference = packageReferenceDao.findByTrackingNumber(trackingNumber);
        if (dbReference != null) {
            applyPackageReference(pkg, dbReference);
            pkg.setReferenceSource(REFERENCE_SOURCE_DB);
            finalizePackage(pkg);
            packageDao.singleUpdate(pkg);
            return RecheckOutcome.DB;
        }

        pkg.setReferenceSource(null);
        pkg.setIsUnderdeclared(null);
        pkg.setStatus(PENDING);
        packageDao.singleUpdate(pkg);
        return RecheckOutcome.MISSING;
    }

    private RecheckOutcome recheckSessionPackage(SessionPackage sessionPackage, Map<String, PackageReference> apiReferences) {
        String trackingNumber = sessionPackage.getTrackingNumber();
        PackageReference apiReference = apiReferences.get(trackingNumber);
        if (apiReference != null) {
            applySessionPackageReference(sessionPackage, apiReference);
            sessionPackage.setReferenceSource(REFERENCE_SOURCE_API);
            finalizeSessionPackage(sessionPackage);
            sessionPackageDao.singleUpdate(sessionPackage);
            publishSessionPackageUpdatedAfterCommit(trackingNumber);
            return RecheckOutcome.API;
        }

        PackageReference dbReference = packageReferenceDao.findByTrackingNumber(trackingNumber);
        if (dbReference != null) {
            applySessionPackageReference(sessionPackage, dbReference);
            sessionPackage.setReferenceSource(REFERENCE_SOURCE_DB);
            finalizeSessionPackage(sessionPackage);
            sessionPackageDao.singleUpdate(sessionPackage);
            publishSessionPackageUpdatedAfterCommit(trackingNumber);
            return RecheckOutcome.DB;
        }

        sessionPackage.setReferenceSource(null);
        sessionPackage.setIsUnderdeclared(null);
        sessionPackage.setStatus(PENDING);
        sessionPackageDao.singleUpdate(sessionPackage);
        publishSessionPackageUpdatedAfterCommit(trackingNumber);
        return RecheckOutcome.MISSING;
    }

    private void finalizePackage(Package pkg) {
        Boolean isUnderdeclared = evaluateIsUnderdeclared(
                pkg.getTrackingNumber(),
                pkg.getShippingMode(),
                pkg.getDeclaredActualWeight(),
                pkg.getAuditedActualWeight(),
                pkg.getAuditedVolumetricWeight()
        );
        pkg.setIsUnderdeclared(isUnderdeclared);
        pkg.setStatus(evaluateFinalStatus(
                pkg.getContainsLiquid(),
                pkg.getContainsBattery(),
                pkg.getItemCondition(),
                pkg.getIsCommercialPackaging(),
                isUnderdeclared,
                pkg.getReferenceSource()
        ));
    }

    private void finalizeSessionPackage(SessionPackage pkg) {
        Boolean isUnderdeclared = evaluateIsUnderdeclared(
                pkg.getTrackingNumber(),
                pkg.getShippingMode(),
                pkg.getDeclaredActualWeight(),
                pkg.getAuditedActualWeight(),
                pkg.getAuditedVolumetricWeight()
        );
        pkg.setIsUnderdeclared(isUnderdeclared);
        pkg.setStatus(evaluateFinalStatus(
                pkg.getContainsLiquid(),
                pkg.getContainsBattery(),
                pkg.getItemCondition(),
                pkg.getIsCommercialPackaging(),
                isUnderdeclared,
                pkg.getReferenceSource()
        ));
    }

    private void applyPackageReference(Package pkg, PackageReference packageReference) {
        FpReferencePayload payload = thirdPartyParcelDetailsService.toPayload(packageReference);
        pkg.setShipmentId(payload.getShipmentId());
        pkg.setDeclaredActualWeight(payload.getDeclaredActualWeight());
        pkg.setDeclaredChargeableWeight(payload.getDeclaredChargeableWeight());
        pkg.setDeclaredVolumetricWeight(payload.getDeclaredVolumetricWeight());
        pkg.setDeclaredLength(payload.getDeclaredLength());
        pkg.setDeclaredWidth(payload.getDeclaredWidth());
        pkg.setDeclaredHeight(payload.getDeclaredHeight());
        pkg.setClientPaidWeight(payload.getClientPaidWeight());
        pkg.setDestinationCountry(payload.getDestinationCountry());
        pkg.setItemCondition(payload.getItemCondition());
        pkg.setContainsLiquid(payload.getContainsLiquid());
        pkg.setContainsBattery(payload.getContainsBattery());
        pkg.setIsCommercialPackaging(payload.getIsCommercialPackaging());
        pkg.setShippingMode(payload.getShippingMode());
        pkg.setServiceProviderName(payload.getServiceProviderName());
    }

    private void applySessionPackageReference(SessionPackage pkg, PackageReference packageReference) {
        FpReferencePayload payload = thirdPartyParcelDetailsService.toPayload(packageReference);
        pkg.setShipmentId(payload.getShipmentId());
        pkg.setDeclaredActualWeight(payload.getDeclaredActualWeight());
        pkg.setDeclaredChargeableWeight(payload.getDeclaredChargeableWeight());
        pkg.setDeclaredVolumetricWeight(payload.getDeclaredVolumetricWeight());
        pkg.setDeclaredLength(payload.getDeclaredLength());
        pkg.setDeclaredWidth(payload.getDeclaredWidth());
        pkg.setDeclaredHeight(payload.getDeclaredHeight());
        pkg.setClientPaidWeight(payload.getClientPaidWeight());
        pkg.setDestinationCountry(payload.getDestinationCountry());
        pkg.setItemCondition(payload.getItemCondition());
        pkg.setContainsLiquid(payload.getContainsLiquid());
        pkg.setContainsBattery(payload.getContainsBattery());
        pkg.setIsCommercialPackaging(payload.getIsCommercialPackaging());
        pkg.setShippingMode(payload.getShippingMode());
        pkg.setServiceProviderName(payload.getServiceProviderName());
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

    private void publishSessionPackageUpdatedAfterCommit(String trackingNumber) {
        scanSessionSseService.publishPackageIngested(trackingNumber);
    }

    private String normalizeContext(String context) {
        if (StringUtils.equalsAnyIgnoreCase(context, CONTEXT_WEIGHT, "package", "packages")) {
            return CONTEXT_WEIGHT;
        }
        if (StringUtils.equalsAnyIgnoreCase(context, CONTEXT_SCAN_SESSION, "scan_session", "session", "session-package", "session-package")) {
            return CONTEXT_SCAN_SESSION;
        }
        return null;
    }

    private enum RecheckOutcome {
        API,
        DB,
        MISSING
    }
}
