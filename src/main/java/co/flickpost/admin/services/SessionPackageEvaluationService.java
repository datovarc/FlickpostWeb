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

@Service
public class SessionPackageEvaluationService {

    private static final String ON_HOLD = "ON_HOLD";
    private static final String PROCEED = "PROCEED";
    private static final String PENDING = "PENDING";
    private static final String REFERENCE_SOURCE_DB = "DB";

    @Autowired
    private SessionPackageDao sessionPackageDao;

    @Autowired
    private PackageReferenceDao packageReferenceDao;

    @Transactional
    public SessionPackageEvaluationResponse evaluate(SessionPackageEvaluationRequest request) {
        validate(request);

        SessionPackage sessionPackage = sessionPackageDao.findSessionPackageByTrackingNumber(request.getTrackingNumber());
        if (sessionPackage == null) {
            throw new IllegalArgumentException("SessionPackage not found for trackingNumber: " + request.getTrackingNumber());
        }

        if (Boolean.TRUE.equals(request.getIsFilled())) {
            String finalStatus = determineFinalStatus(
                    sessionPackage.getContainsLiquid(),
                    sessionPackage.getContainsBattery(),
                    sessionPackage.getItemCondition(),
                    sessionPackage.getIsCommercialPackaging(),
                    sessionPackage.getShippingMode(),
                    sessionPackage.getDeclaredWeight(),
                    sessionPackage.getAuditedWeight(),
                    sessionPackage.getAuditedVolumetricWeight()
            );

            sessionPackage.setStatus(finalStatus);
            sessionPackageDao.singleUpdate(sessionPackage);

            return new SessionPackageEvaluationResponse(
                    true,
                    sessionPackage.getTrackingNumber(),
                    finalStatus,
                    sessionPackage.getReferenceSource(),
                    false,
                    "SessionPackage evaluated successfully"
            );
        }

        PackageReference packageReference = packageReferenceDao.findByTrackingNumber(request.getTrackingNumber());
        if (packageReference == null) {
            sessionPackage.setStatus(PENDING);
            sessionPackageDao.singleUpdate(sessionPackage);
            return new SessionPackageEvaluationResponse(
                    true,
                    sessionPackage.getTrackingNumber(),
                    PENDING,
                    sessionPackage.getReferenceSource(),
                    false,
                    "PackageReference not found. SessionPackage set to PENDING"
            );
        }

        sessionPackage.setShippingMode(packageReference.getShippingMode());
        sessionPackage.setContainsLiquid(packageReference.getContainsLiquid());
        sessionPackage.setContainsBattery(packageReference.getContainsBattery());
        sessionPackage.setItemCondition(packageReference.getItemCondition());
        sessionPackage.setIsCommercialPackaging(packageReference.getIsCommercialPackaging());
        sessionPackage.setDeclaredWeight(packageReference.getDeclaredWeight());
        sessionPackage.setDeclaredLength(packageReference.getDeclaredLength());
        sessionPackage.setDeclaredWidth(packageReference.getDeclaredWidth());
        sessionPackage.setDeclaredHeight(packageReference.getDeclaredHeight());
        sessionPackage.setClientPaidHeight(packageReference.getClientPaidHeight());
        sessionPackage.setShipmentId(packageReference.getShipmentId());
        sessionPackage.setReferenceSource(REFERENCE_SOURCE_DB);

        String finalStatus = determineFinalStatus(
                sessionPackage.getContainsLiquid(),
                sessionPackage.getContainsBattery(),
                sessionPackage.getItemCondition(),
                sessionPackage.getIsCommercialPackaging(),
                sessionPackage.getShippingMode(),
                sessionPackage.getDeclaredWeight(),
                sessionPackage.getAuditedWeight(),
                sessionPackage.getAuditedVolumetricWeight()
        );

        sessionPackage.setStatus(finalStatus);
        sessionPackageDao.singleUpdate(sessionPackage);

        return new SessionPackageEvaluationResponse(
                true,
                sessionPackage.getTrackingNumber(),
                finalStatus,
                REFERENCE_SOURCE_DB,
                true,
                "SessionPackage evaluated successfully"
        );
    }

    private void validate(SessionPackageEvaluationRequest request) {
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

    private String determineFinalStatus(Boolean containsLiquid,
                                        Boolean containsBattery,
                                        String itemCondition,
                                        Boolean isCommercialPackaging,
                                        String shippingMode,
                                        java.math.BigDecimal declaredWeight,
                                        java.math.BigDecimal auditedWeight,
                                        java.math.BigDecimal auditedVolumetricWeight) {
        if (Boolean.TRUE.equals(containsLiquid)
                || Boolean.TRUE.equals(containsBattery)
                || isUsed(itemCondition)
                || Boolean.FALSE.equals(isCommercialPackaging)) {
            return ON_HOLD;
        }

        if (declaredWeight == null || auditedWeight == null || StringUtils.isBlank(shippingMode)) {
            return PROCEED;
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
            return PROCEED;
        }

        if (declared < evaluationWeight) {
            return ON_HOLD;
        }

        return PROCEED;
    }

    private boolean isUsed(String itemCondition) {
        return StringUtils.isNotBlank(itemCondition) && !"new".equalsIgnoreCase(itemCondition.trim());
    }
}
