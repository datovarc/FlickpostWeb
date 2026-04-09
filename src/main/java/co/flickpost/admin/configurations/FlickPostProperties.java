package co.flickpost.admin.configurations;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class FlickPostProperties {
    private static final Logger logger = LogManager.getLogger(FlickPostProperties.class);

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}")
    private Integer uploadBatchSize;
    @Value("${image.upload.path}")
    private String imageUploadPath;
    @Value("${image.upload.url}")
    private String imageUploadUrl;
    @Value("#{'${packages.images.possible.statuses}'.split(',')}")
    private List<String> packageImageStatuses;
    @Value("${scan.session.evaluation.api.key}")
    private String scanSessionEvaluationApiKey;
    @Value("${thirdparty.parcel.details.url:}")
    private String thirdPartyParcelDetailsUrl;
    @Value("${thirdparty.api.key:}")
    private String thirdPartyApiKey;
    @Value("${thirdparty.hub.received.url:}")
    private String thirdPartyHubReceivedUrl;
    @Value("${thirdparty.audited.values.url:}")
    private String thirdPartyAuditedValuesUrl;
    @Value("#{${scan.session.add-status.main-status.options:{'ARRIVED_AT_HUB':'Arrived At Hub'}}}")
    private Map<String, String> scanSessionAddStatusMainStatusOptions;
    @Value("#{${scan.session.add-status.sub-status.options.by-main-status:{'ARRIVED_AT_HUB':'AWAITING_PARCEL_AUDIT:Awaiting parcel audit|ON_HOLD:On hold|DG_PACKAGING_REQUIRED:Require DG packaging|PROCEED_TO_NEXT_STAGE:Proceed|CONSOLIDATED:Consolidate|RETURNED:Returned|UNKNOWN_STATUS:Unknown'}}}")
    private Map<String, String> scanSessionAddStatusSubStatusOptionsByMainStatus;

    @PostConstruct
    public void initializeApplication() {

        logger.info("batch_size: " + this.uploadBatchSize);
        logger.info("image.upload.path: {}", this.imageUploadPath);
        logger.info("image.upload.url: {}", this.imageUploadUrl);
        logger.info("packages.images.possible.statuses: {}", this.packageImageStatuses);
        logger.info("thirdparty.parcel.details.url: {}", this.thirdPartyParcelDetailsUrl);
        logger.info("thirdparty.api.key configured: {}", this.thirdPartyApiKey != null && !this.thirdPartyApiKey.isEmpty());
        logger.info("thirdparty.hub.received.url: {}", this.thirdPartyHubReceivedUrl);
        logger.info("thirdparty.audited.values.url: {}", this.thirdPartyAuditedValuesUrl);
        logger.info("scan.session.add-status.main-status.options: {}", this.scanSessionAddStatusMainStatusOptions);
        logger.info("scan.session.add-status.sub-status.options.by-main-status: {}", this.scanSessionAddStatusSubStatusOptionsByMainStatus);
        logger.info("Properties loaded");
    }

    public Integer getUploadBatchSize() {
        return uploadBatchSize;
    }

    public void setUploadBatchSize(Integer uploadBatchSize) {
        this.uploadBatchSize = uploadBatchSize;
    }

    public String getImageUploadPath() {
        return imageUploadPath;
    }

    public void setImageUploadPath(String imageUploadPath) {
        this.imageUploadPath = imageUploadPath;
    }

    public String getImageUploadUrl() {
        return imageUploadUrl;
    }

    public void setImageUploadUrl(String imageUploadUrl) {
        this.imageUploadUrl = imageUploadUrl;
    }

    public List<String> getPackageImageStatuses() {
        return packageImageStatuses;
    }

    public void setPackageImageStatuses(List<String> packageImageStatuses) {
        this.packageImageStatuses = packageImageStatuses;
    }

    public String getScanSessionEvaluationApiKey() {
        return scanSessionEvaluationApiKey;
    }

    public void setScanSessionEvaluationApiKey(String scanSessionEvaluationApiKey) {
        this.scanSessionEvaluationApiKey = scanSessionEvaluationApiKey;
    }

    public String getThirdPartyParcelDetailsUrl() {
        return thirdPartyParcelDetailsUrl;
    }

    public void setThirdPartyParcelDetailsUrl(String thirdPartyParcelDetailsUrl) {
        this.thirdPartyParcelDetailsUrl = thirdPartyParcelDetailsUrl;
    }

    public String getThirdPartyApiKey() {
        return thirdPartyApiKey;
    }

    public void setThirdPartyApiKey(String thirdPartyApiKey) {
        this.thirdPartyApiKey = thirdPartyApiKey;
    }

    public String getThirdPartyHubReceivedUrl() {
        return thirdPartyHubReceivedUrl;
    }

    public void setThirdPartyHubReceivedUrl(String thirdPartyHubReceivedUrl) {
        this.thirdPartyHubReceivedUrl = thirdPartyHubReceivedUrl;
    }

    public String getThirdPartyAuditedValuesUrl() {
        return thirdPartyAuditedValuesUrl;
    }

    public void setThirdPartyAuditedValuesUrl(String thirdPartyAuditedValuesUrl) {
        this.thirdPartyAuditedValuesUrl = thirdPartyAuditedValuesUrl;
    }

    public Map<String, String> getScanSessionAddStatusMainStatusOptions() {
        if (scanSessionAddStatusMainStatusOptions == null) {
            return Collections.emptyMap();
        }
        return new LinkedHashMap<>(scanSessionAddStatusMainStatusOptions);
    }

    public void setScanSessionAddStatusMainStatusOptions(Map<String, String> scanSessionAddStatusMainStatusOptions) {
        this.scanSessionAddStatusMainStatusOptions = scanSessionAddStatusMainStatusOptions;
    }

    public List<Map<String, String>> getScanSessionAddStatusMainStatusOptionEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        if (scanSessionAddStatusMainStatusOptions == null) {
            return entries;
        }
        for (Map.Entry<String, String> entry : scanSessionAddStatusMainStatusOptions.entrySet()) {
            Map<String, String> option = new LinkedHashMap<>();
            option.put("value", entry.getKey());
            option.put("label", entry.getValue());
            entries.add(option);
        }
        return entries;
    }

    public Map<String, List<Map<String, String>>> getScanSessionAddStatusSubStatusOptionEntriesByMainStatus() {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        if (scanSessionAddStatusSubStatusOptionsByMainStatus == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : scanSessionAddStatusSubStatusOptionsByMainStatus.entrySet()) {
            result.put(entry.getKey(), parseOptionList(entry.getValue()));
        }
        return result;
    }

    private List<Map<String, String>> parseOptionList(String rawValue) {
        List<Map<String, String>> entries = new ArrayList<>();
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return entries;
        }

        String[] options = rawValue.split("\\|");
        for (String optionValue : options) {
            if (optionValue == null) {
                continue;
            }
            String trimmedOption = optionValue.trim();
            if (trimmedOption.isEmpty()) {
                continue;
            }

            String[] parts = trimmedOption.split(":", 2);
            String value = parts[0].trim();
            String label = parts.length > 1 ? parts[1].trim() : value;
            if (value.isEmpty()) {
                continue;
            }

            Map<String, String> option = new LinkedHashMap<>();
            option.put("value", value);
            option.put("label", label);
            entries.add(option);
        }
        return entries;
    }
}
