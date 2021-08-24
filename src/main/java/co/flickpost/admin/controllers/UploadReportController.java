package co.flickpost.admin.controllers;


import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.helpers.ExcelHelper;
import co.flickpost.admin.models.Shipment;
import co.flickpost.admin.repositories.ShipmentDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;


@Controller
@RequestMapping("/report")
public class UploadReportController {

    @Autowired
    ShipmentDao shipmentDao;
    @Autowired
    FlickPostProperties properties;

    private static final Logger logger = LogManager.getLogger(UploadReportController.class);

    @PostMapping(value = "/upload")
    public String fileUpload(@RequestParam("file") MultipartFile file, @RequestHeader String referer, Principal principal) {
        String uid = principal.getName();
        logger.info("{} - Started processing Report file.", uid);
        if (file.isEmpty()) {
            logger.info("{} - Report file was empty.", uid);
            return "redirect:" + referer;
        }

        try {

            logger.info("{} - Started Analyzing Report file.", uid);
            HSSFWorkbook wb  = new HSSFWorkbook(file.getInputStream());
            List<Shipment> shipments = ExcelHelper.readShipments(wb);
            logger.info("{} - Finished Analyzing Report file. There were a total of: " + shipments.size() + " Records.", uid);

            if(shipments != null && !shipments.isEmpty()){
                //update DB
                logger.info("{} - Started writing records in DB.", uid);
                int batchSize = properties.getUploadBatchSize();
                shipmentDao.batchUpdate(shipments, batchSize);
                logger.info("{} - Finished writing records in DB.", uid);
            }

        } catch (Exception e) {
            logger.error("{} - Error processing Report file.", uid);
            return "redirect:" + referer;
        }

        return "redirect:" + referer;
    }

}
