package co.flickpost.admin.services;

import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.helpers.ImageHelper;
import co.flickpost.admin.helpers.PackageHelper;
import co.flickpost.admin.models.ImageInfo;
import co.flickpost.admin.models.PackageReference;
import co.flickpost.admin.models.ScanSession;
import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.FpReferencePayload;
import co.flickpost.admin.models.json.FpSessionPackageIngestRequest;
import co.flickpost.admin.models.json.WeightProcessResponse;
import co.flickpost.admin.repositories.ScanSessionDao;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class InlineWeightProcessService {

    private static final Logger logger = LogManager.getLogger(InlineWeightProcessService.class);
    private static final String ACTIVE = "ACTIVE";
    private static final String DEFAULT_HUB = "KLG";
    private static final String DEFAULT_STATUS = "Processing at Hub";
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private ScanSessionDao scanSessionDao;

    @Autowired
    private PackageReferenceService packageReferenceService;

    @Autowired
    private SessionPackageIngestService sessionPackageIngestService;

    @Autowired
    private FlickPostProperties properties;

    public WeightProcessResponse processIncoming(SessionPackage packageInfo) {
        WeightProcessResponse response = new WeightProcessResponse();
        String trackingNumber = packageInfo.getTrackingNumber();

        logger.info("Received weighting information for package: {}", trackingNumber);
        logger.info("Started processing at: {}", sdf.format(new Date()));

        Optional<ScanSession> activeSessionOptional = scanSessionDao.findFirstByStatusOrderByStartTimeDesc(ACTIVE);
        if (!activeSessionOptional.isPresent()) {
            logger.info("There is no active scan session, aborting processing for {}", trackingNumber);
            response.setCode(0);
            response.setError("There is no active scan session, aborting processing.");
            return response;
        }

        Long activeSessionId = activeSessionOptional.get().getId();
        logger.info("Started processing {} information with active session {}.", trackingNumber, activeSessionId);

        try {
            byte[] decodedImg = Base64.getDecoder().decode(packageInfo.getEncodedImage());

            if (StringUtils.isNotEmpty(properties.getImageUploadPath())) {
                String destinationUrl = ImageHelper.writeFile(properties.getImageUploadPath(), properties.getImageUploadUrl(), packageInfo, decodedImg);
                List<ImageInfo> imageInfos = List.of(new ImageInfo(DEFAULT_STATUS, destinationUrl));
                packageInfo.setImageInfos(imageInfos);
            }

            PackageHelper.updateMmToCm(packageInfo);
            PackageHelper.updateVolumetricWeight(packageInfo);
            PackageHelper.updateChargeableWeight(packageInfo);
            packageInfo.setHub(DEFAULT_HUB);
            packageInfo.setSessionId(activeSessionId);

            Optional<PackageReference> packageReferenceOptional = packageReferenceService.fetchAndUpsert(trackingNumber);
            FpSessionPackageIngestRequest request = buildIngestRequest(packageInfo, packageReferenceOptional);
            sessionPackageIngestService.ingest(request);

            response.setCode(0);
            response.setError("Package " + trackingNumber + " processed successfully.");
        } catch (Exception e) {
            logger.error("Failed to process {} information in FP.", trackingNumber, e);
            response.setCode(-1);
            response.setError("Failed to process package " + trackingNumber + " in FP");

            logger.info("Finished processing at: {}", sdf.format(new Date()));
            logger.info("Result: {}", response);
            return response;
        }

        logger.info("Finished processing at: {}", sdf.format(new Date()));
        logger.info("Result: {}", response);
        return response;
    }

    private FpSessionPackageIngestRequest buildIngestRequest(SessionPackage packageInfo, Optional<PackageReference> packageReferenceOptional) {
        FpSessionPackageIngestRequest request = new FpSessionPackageIngestRequest();
        request.setIsFilled(packageReferenceOptional.isPresent());
        request.setTrackingNumber(packageInfo.getTrackingNumber());
        request.setHub(packageInfo.getHub());
        request.setAuditedLength(packageInfo.getAuditedLength());
        request.setAuditedWidth(packageInfo.getAuditedWidth());
        request.setAuditedHeight(packageInfo.getAuditedHeight());
        request.setAuditedWeight(packageInfo.getAuditedWeight());
        request.setAuditedVolumetricWeight(packageInfo.getAuditedVolumetricWeight());
        request.setChargeableWeight(packageInfo.getChargeableWeight());
        request.setHid(packageInfo.getHid());
        request.setDateTime(packageInfo.getDateTime());
        request.setSessionId(packageInfo.getSessionId());

        if (packageReferenceOptional.isPresent()) {
            PackageReference packageReference = packageReferenceOptional.get();
            FpReferencePayload reference = new FpReferencePayload();
            reference.setShipmentId(packageReference.getShipmentId());
            reference.setDeclaredWeight(packageReference.getDeclaredWeight());
            reference.setDeclaredLength(packageReference.getDeclaredLength());
            reference.setDeclaredWidth(packageReference.getDeclaredWidth());
            reference.setDeclaredHeight(packageReference.getDeclaredHeight());
            reference.setClientPaidHeight(packageReference.getClientPaidHeight());
            reference.setItemCondition(packageReference.getItemCondition());
            reference.setContainsLiquid(packageReference.getContainsLiquid());
            reference.setContainsBattery(packageReference.getContainsBattery());
            reference.setIsCommercialPackaging(packageReference.getIsCommercialPackaging());
            reference.setShippingMode(packageReference.getShippingMode());
            request.setReference(reference);
        }

        return request;
    }
}
