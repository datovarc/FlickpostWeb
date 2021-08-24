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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.SimpleDateFormat;
import java.util.List;


@Controller
@RequestMapping("/report")
public class UploadReportController {

    @Autowired
    ShipmentDao shipmentDao;
    @Autowired
    FlickPostProperties properties;

    private static final Logger logger = LogManager.getLogger(UploadReportController.class);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @PostMapping(value = "/upload")
    public String fileUpload(@RequestParam("file") MultipartFile file, @RequestHeader String referer) {

        if (file.isEmpty()) {
            return "redirect:" + referer;
        }

        try {

            HSSFWorkbook wb  = new HSSFWorkbook(file.getInputStream());
            List<Shipment> shipments = ExcelHelper.readShipments(wb);

            if(shipments != null && !shipments.isEmpty()){
                //update DB
                int batchSize = properties.getUploadBatchSize();
                shipmentDao.batchUpdate(shipments, batchSize);
            }

        } catch (Exception e) {
            logger.error("Error processing file.", e);
            return "redirect:" + referer;
        }

        return "redirect:" + referer;
    }

}
