package co.flickpost.admin.helpers;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import co.flickpost.admin.models.Package;
import org.imgscalr.Scalr;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

public class ImageHelper {

    private static final Logger logger = LogManager.getLogger(ImageHelper.class);

    private static final int MAX_HEIGHT = 960;
    private static final int MAX_WIDTH = 1280;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    public static void renameDirectory(String path, String originalTrackingNumber, String newTrackingNumber){

        String oldDirectoryName = path.concat(originalTrackingNumber);
        File oldDirectory = new File(oldDirectoryName);
        if (!oldDirectory.exists()) return;

        String newDirectoryName = path.concat(newTrackingNumber);
        File newDirectory = new File(newDirectoryName);

        oldDirectory.renameTo(newDirectory);

        File[] oldFiles = newDirectory.listFiles();
        if(oldFiles == null) return;

        for(File oldFile : oldFiles){
            String oldFileName = oldFile.getName();
            String newFileName = oldFileName.replace(originalTrackingNumber, newTrackingNumber);
            String name = oldFile.getPath().replace(oldFileName, newFileName);
            File newFile = new File(name);
            oldFile.renameTo(newFile);
        }

    }

    public static String writeFile(String path, String serverUrl, Package packageInfo, MultipartFile imgFile){
        Date dateNow = new Date();

        String format = imgFile.getContentType().split("/")[1];

        String resultingUrl = "";
        String directoryName = path.concat(packageInfo.getTrackingNumber());
        String fileName = packageInfo.getTrackingNumber() + "_MU" + sdf.format(dateNow) + "." + format;

        File directory = new File(directoryName);
        if (!directory.exists()){
            directory.mkdir();
        }

        try{
            Path destinationFile = Paths.get(directoryName + "/", fileName);
            Files.write(destinationFile, imgFile.getBytes());
            resultingUrl = serverUrl + packageInfo.getTrackingNumber() + "/" + fileName;

            BufferedImage bufferedImage = ImageIO.read(destinationFile.toFile());
            BufferedImage outputImage;
            if(bufferedImage.getHeight() > bufferedImage.getWidth()){
                outputImage = Scalr.resize(bufferedImage, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_TO_HEIGHT, MAX_HEIGHT, Scalr.OP_ANTIALIAS);
            } else{
                outputImage = Scalr.resize(bufferedImage, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_TO_WIDTH, MAX_WIDTH, Scalr.OP_ANTIALIAS);
            }

            File newImageFile = destinationFile.toFile();
            ImageIO.write(outputImage, format, newImageFile);
            outputImage.flush();

        }
        catch (IOException e){
            logger.error("Could not upload image for: {}", packageInfo.getTrackingNumber());
        }

        return resultingUrl;
    }

    public static boolean delete(String path, List<String> trackingNumbers){
        for(String trackingNumber : trackingNumbers){
            String directoryName = path.concat(trackingNumber);
            File directory = new File(directoryName);
            if (!directory.exists()) continue;
            try {
                FileUtils.deleteDirectory(directory);
            } catch(IOException io){
                logger.error("Could not delete image for: {}", trackingNumber);
            }
        }

        return true;

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
