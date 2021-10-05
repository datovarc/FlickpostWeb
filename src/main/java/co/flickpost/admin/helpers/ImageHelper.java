package co.flickpost.admin.helpers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import co.flickpost.admin.models.Package;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public class ImageHelper {

    private static final Logger logger = LogManager.getLogger(ImageHelper.class);

    public static String writeFile(String path, Package packageInfo, MultipartFile imgFile){

        String format = imgFile.getContentType().split("/")[1];
        int index = 0;

        String resultingPath = "";
        String directoryName = path.concat(packageInfo.getTrackingNumber());

        File directory = new File(directoryName);
        if (!directory.exists()){
            directory.mkdir();
        }

        if(directory.list() != null){
            index = directory.list().length;
        }

        String fileName = packageInfo.getTrackingNumber() + "_MU" + index + "." + format;

        try{
            Path destinationFile = Paths.get(directoryName + "/", fileName);
            Files.write(destinationFile, imgFile.getBytes());
            resultingPath = destinationFile.toString();
        }
        catch (IOException e){
            logger.error("Could not upload image for: {}", packageInfo.getTrackingNumber());
        }

        return resultingPath;
    }

    public static MediaType determineMediaType(String filename){
        String[] splitted = filename.split("\\.");
        int indexes = splitted.length;
        String extension = splitted[indexes-1].toLowerCase();

        switch (extension){
            case "png":
                return MediaType.IMAGE_PNG;
            case "jpg":
                return MediaType.IMAGE_JPEG;
            case "gif":
                return MediaType.IMAGE_GIF;
            default:
                return MediaType.IMAGE_JPEG;
        }
    }
}
