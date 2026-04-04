package co.flickpost.admin.helpers;

import co.flickpost.admin.models.SessionPackage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImageHelper {

    private static final Logger logger = LogManager.getLogger(ImageHelper.class);

    private ImageHelper() {}

    public static String writeFile(String path, String serverUrl, SessionPackage packageInfo, byte[] decodedImg) {
        String resultingUrl = "";
        String directoryName = path.concat(packageInfo.getTrackingNumber());
        String fileName = packageInfo.getTrackingNumber() + "_WM0.jpg";

        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdir();
        }

        try {
            Path destinationFile = Paths.get(directoryName + "/", fileName);
            Files.write(destinationFile, decodedImg);
            resultingUrl = serverUrl + packageInfo.getTrackingNumber() + "/" + fileName;
        } catch (IOException e) {
            logger.error("Could not upload image for: {}", packageInfo.getTrackingNumber(), e);
        }

        return resultingUrl;
    }
}
