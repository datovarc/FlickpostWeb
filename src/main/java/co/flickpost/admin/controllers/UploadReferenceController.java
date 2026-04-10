package co.flickpost.admin.controllers;

import co.flickpost.admin.helpers.ExcelHelper;
import co.flickpost.admin.models.PackageReference;
import co.flickpost.admin.repositories.PackageReferenceDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
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
@RequestMapping("/reference")
public class UploadReferenceController {

    private static final Logger logger = LogManager.getLogger(UploadReferenceController.class);

    @Autowired
    private PackageReferenceDao packageReferenceDao;

    @PostMapping("/upload")
    public String fileUpload(@RequestParam("file") MultipartFile file, @RequestHeader String referer, Principal principal) {
        String uid = principal.getName();
        logger.info("{} - Started processing Package Reference file.", uid);

        if (file.isEmpty()) {
            logger.info("{} - Package Reference file was empty.", uid);
            return "redirect:" + referer;
        }

        try (Workbook workbook = ExcelHelper.readWorkbook(file.getInputStream())) {
            List<PackageReference> packageReferences = ExcelHelper.readPackageReferences(workbook);
            logger.info("{} - Finished analyzing Package Reference file. There were a total of: {} Records.", uid, packageReferences.size());

            for (PackageReference packageReference : packageReferences) {
                if (packageReference.getTrackingNumber() == null) {
                    continue;
                }

                PackageReference existing = packageReferenceDao.findByTrackingNumber(packageReference.getTrackingNumber());
                if (existing != null) {
                    packageReference.setId(existing.getId());
                }
                packageReferenceDao.save(packageReference);
            }

            logger.info("{} - Finished writing Package Reference records in DB.", uid);
        } catch (Exception e) {
            logger.error("{} - Error processing Package Reference file.", uid, e);
        }

        return "redirect:" + referer;
    }
}
