package co.flickpost.admin.helpers;

import co.flickpost.admin.models.SessionPackage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PackageHelper {

    private static final Logger logger = LogManager.getLogger(PackageHelper.class);

    private PackageHelper() {}

    public static void updateMmToCm(SessionPackage pkg) {
        logger.info("Started converting MMs to CMs for : {}", pkg.getTrackingNumber());

        if (pkg.getAuditedHeight() == null || pkg.getAuditedLength() == null || pkg.getAuditedWidth() == null) {
            logger.info("Could not convert MMs to CMs for : {}", pkg.getTrackingNumber());
            return;
        }

        logger.info("Original values : {} L x {} W x {} H", pkg.getAuditedLength(), pkg.getAuditedWidth(), pkg.getAuditedHeight());
        BigDecimal auditedLength = pkg.getAuditedLength().divide(BigDecimal.valueOf(10));
        BigDecimal auditedWidth = pkg.getAuditedWidth().divide(BigDecimal.valueOf(10));
        BigDecimal auditedHeight = pkg.getAuditedHeight().divide(BigDecimal.valueOf(10));

        pkg.setAuditedLength(auditedLength);
        pkg.setAuditedWidth(auditedWidth);
        pkg.setAuditedHeight(auditedHeight);
        logger.info("Updated values : {} L x {} W x {} H", pkg.getAuditedLength(), pkg.getAuditedWidth(), pkg.getAuditedHeight());

        logger.info("Finished converting MMs to CMs for : {}", pkg.getTrackingNumber());
    }

    public static void updateVolumetricWeight(SessionPackage pkg) {
        logger.info("Started calculating Audited Volumetric Weight for: {}", pkg.getTrackingNumber());
        if (pkg.getAuditedHeight() == null || pkg.getAuditedLength() == null || pkg.getAuditedWidth() == null) {
            logger.info("Could not calculate Audited Volumetric Weight for: {}", pkg.getTrackingNumber());
            return;
        }

        logger.info("With dimensions : {} L x {} W x {} H", pkg.getAuditedLength(), pkg.getAuditedWidth(), pkg.getAuditedHeight());

        BigDecimal volumetricWeight = pkg.getAuditedLength()
                .multiply(pkg.getAuditedWidth())
                .multiply(pkg.getAuditedHeight())
                .divide(BigDecimal.valueOf(5000))
                .setScale(2, RoundingMode.UP);

        pkg.setAuditedVolumetricWeight(volumetricWeight);
        logger.info("Finished calculating Audited Volumetric Weight for: {} -> {}", pkg.getTrackingNumber(), volumetricWeight);
    }

    public static void updateChargeableWeight(SessionPackage pkg) {
        logger.info("Started determining Chargeable Weight for: {}", pkg.getTrackingNumber());
        if (pkg.getAuditedWeight() == null || pkg.getAuditedVolumetricWeight() == null) {
            logger.info("Could not determine Chargeable Weight for: {}", pkg.getTrackingNumber());
            return;
        }

        BigDecimal chargeableWeight = pkg.getAuditedWeight().compareTo(pkg.getAuditedVolumetricWeight()) > 0
                ? pkg.getAuditedWeight() : pkg.getAuditedVolumetricWeight();

        pkg.setChargeableWeight(chargeableWeight.setScale(1, RoundingMode.UP));
        logger.info("Finished determining Chargeable Weight for: {} -> {}", pkg.getTrackingNumber(), chargeableWeight);
    }
}
