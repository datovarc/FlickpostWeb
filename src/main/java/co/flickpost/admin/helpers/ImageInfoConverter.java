package co.flickpost.admin.helpers;

import co.flickpost.admin.models.ImageInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.List;

public class ImageInfoConverter implements AttributeConverter<List<ImageInfo>, String> {

    private static final Logger logger = LogManager.getLogger(ImageInfoConverter.class);

    @Override
    public String convertToDatabaseColumn(List<ImageInfo> imageInfo) {

        ObjectMapper mapper = new ObjectMapper();

        String customerInfoJson = null;
        try {
            customerInfoJson = mapper.writeValueAsString(imageInfo);
        } catch (final JsonProcessingException e) {
            logger.error("JSON writing error", e);
        }

        return customerInfoJson;
    }

    @Override
    public List<ImageInfo> convertToEntityAttribute(String imageInfoJSON) {

        if(imageInfoJSON == null) return null;

        List<ImageInfo> imageInfo = null;
        ObjectMapper mapper = new ObjectMapper();

        try {
            imageInfo = mapper.readValue(imageInfoJSON, List.class);
        } catch (final IOException e) {
            logger.error("JSON reading error", e);
        }

        return imageInfo;
    }

}
