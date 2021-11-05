package co.flickpost.admin.configurations;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

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

    @PostConstruct
    public void initializeApplication() {

        logger.info("batch_size: " + this.uploadBatchSize);
        logger.info("image.upload.path: {}", this.imageUploadPath);
        logger.info("image.upload.url: {}", this.imageUploadUrl);
        logger.info("packages.images.possible.statuses: {}", this.packageImageStatuses);
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
}