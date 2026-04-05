package co.flickpost.admin.helpers;


import co.flickpost.admin.models.Package;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PackageHelper {

    private static final Logger logger = LogManager.getLogger(PackageHelper.class);

    public static void updateVolumetricWeight(Package pkg){
        logger.info("Started calculating Audited Volumetric Weight for: " + pkg.getTrackingNumber());
        if(pkg.getAuditedHeight() == null || pkg.getAuditedLength() == null || pkg.getAuditedWidth() == null || pkg.getAuditedVolumetricWeight() != null){
            return;
        }

        BigDecimal volumetricWeight;
        BigDecimal auditedLength = pkg.getAuditedLength();
        BigDecimal auditedWidth = pkg.getAuditedWidth();
        BigDecimal auditedHeight = pkg.getAuditedHeight();

        volumetricWeight = auditedLength
                .multiply(auditedWidth)
                .multiply(auditedHeight)
                .divide(BigDecimal.valueOf(5000)).setScale(2, RoundingMode.UP);

        pkg.setAuditedVolumetricWeight(volumetricWeight);

        logger.info("Finished calculating Audited Volumetric Weight for: " + pkg.getTrackingNumber() + " -> " + volumetricWeight.toString());
    }

    public static void updateChargeableWeight(Package pkg){
        logger.info("Started determining Chargeable Weight for: " + pkg.getTrackingNumber());
        if(pkg.getAuditedWeight() == null || pkg.getAuditedVolumetricWeight() == null || pkg.getAuditedChargeableWeight() != null){
            return;
        }

        BigDecimal chargeableWeight;
        BigDecimal auditedWeight = pkg.getAuditedWeight();
        BigDecimal auditedVolumetricWeight = pkg.getAuditedVolumetricWeight();

        chargeableWeight = auditedWeight.compareTo(auditedVolumetricWeight) == 1
                ? auditedWeight : auditedVolumetricWeight;

        pkg.setAuditedChargeableWeight(chargeableWeight.setScale(1, RoundingMode.UP));
        logger.info("Finished determining Chargeable Weight for: " + pkg.getTrackingNumber() + " -> " + chargeableWeight.toString());

    }

    public static void applyRounding(List<Package> packages){
        for(Package pkg : packages){
            pkg.setAuditedHeight(pkg.getAuditedHeight().setScale(2, RoundingMode.UP));
            pkg.setAuditedLength(pkg.getAuditedLength().setScale(2, RoundingMode.UP));
            pkg.setAuditedWeight(pkg.getAuditedWeight().setScale(2, RoundingMode.UP));
            pkg.setAuditedWidth(pkg.getAuditedWidth().setScale(2, RoundingMode.UP));
            pkg.setAuditedVolumetricWeight(pkg.getAuditedVolumetricWeight().setScale(2, RoundingMode.UP));
            pkg.setAuditedChargeableWeight(pkg.getAuditedChargeableWeight().setScale(1, RoundingMode.UP));
        }

    }
}
