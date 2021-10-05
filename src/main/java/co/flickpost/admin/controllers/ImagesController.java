package co.flickpost.admin.controllers;


import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.helpers.ImageHelper;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;


@Controller
@RequestMapping("/img")
public class ImagesController {

    @Autowired
    FlickPostProperties properties;

    private static final Logger logger = LogManager.getLogger(ImagesController.class);

    @CrossOrigin(origins = {"https://flickpost.co", "http://flickpost.co","https://flickpost.com", "http://flickpost.com","https://api.flickpost.com", "http://api.flickpost.com"})
    @GetMapping("/{directory}/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable("directory") String directory, @PathVariable("filename") String filename) {
        byte[] image = new byte[0];
        try {
            image = FileUtils.readFileToByteArray(new File(properties.getImageUploadPath() + directory + "/"+filename));
        } catch (IOException e) {
            logger.error("Couldn't access image {} in directory {}. Are you sure it exists?", filename, directory);
        }

        MediaType mediaType = ImageHelper.determineMediaType(filename);

        return ResponseEntity.ok().contentType(mediaType).body(image);
    }


}
