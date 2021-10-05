package co.flickpost.admin.controllers;


import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.helpers.ExcelHelper;
import co.flickpost.admin.helpers.ImageHelper;
import co.flickpost.admin.helpers.PackageHelper;
import co.flickpost.admin.models.Company;
import co.flickpost.admin.models.ImageInfo;
import co.flickpost.admin.models.json.DownloadRequest;
import co.flickpost.admin.models.Package;
import co.flickpost.admin.models.json.PaginationRequest;
import co.flickpost.admin.models.json.PaginationResponse;
import co.flickpost.admin.repositories.CompanyDao;
import co.flickpost.admin.repositories.PackageDao;
import co.flickpost.admin.security.UserDetailsImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/weight")
public class WeightViewController {

    @Autowired
    PackageDao packageDao;
    @Autowired
    CompanyDao companyDao;
    @Autowired
    FlickPostProperties properties;
    @Autowired
    BCryptPasswordEncoder encoder;

    private static final Logger logger = LogManager.getLogger(WeightViewController.class);

    private final String DEFAULT_STATUS = "Processing at Hub";

    @GetMapping(value = "")
    public String weightView(Model model, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<Company> companies =  companyDao.getAllCompanies();
        companies = companies.stream().filter(company -> !company.equals(userDetails.getCompany())).collect(Collectors.toList());
        model.addAttribute("companies", companies);
        model.addAttribute("defaultCompany", userDetails.getCompany());
        return "weight";
    }


    @PostMapping(value = "/data")
    @ResponseBody
    public PaginationResponse weightPagination(@RequestBody PaginationRequest request, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String uid = userDetails.getUsername();

        logger.info("{} - Started fetching data for Audited Weight View", uid);
        Map<String, Object> packagesPagination = packageDao.pagination(request, userDetails.getCompany().getCode());
        if(packagesPagination == null){
            logger.info("{} - There was no data for Audited Weight View", uid);
            return new PaginationResponse(0, Collections.EMPTY_LIST);
        }

        Long count = (Long) packagesPagination.get("count");
        List<Package> packages = (List) packagesPagination.get("packages");
        PackageHelper.applyRounding(packages);

        logger.info("{} - There was a total of {} packages for Audited Weight View", uid, count);
        logger.info("{} - Currently displaying Page {} for Audited Weight View", uid, request.getPageNumber());

//        logger.info("{} - Started processing images for Audited Weight View", uid);
//        processImages(packages);
//        logger.info("{} - Finished processing images for Audited Weight View", uid);

        int numPages = count.intValue() / request.getPageSize() + ((count.intValue() % request.getPageSize() == 0) ? 0 : 1);
        logger.info("{} - Total of {} pages available for Audited Weight View", uid, numPages);

        PaginationResponse paginationResponse = new PaginationResponse<>(numPages, packages);

        return paginationResponse;
    }

    @PostMapping(value = "/download")
    @ResponseBody
    public ResponseEntity<byte[]> downloadWeight(@RequestBody DownloadRequest request, Authentication authentication) throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String uid = userDetails.getUsername();

        logger.info("{} - Started downloading Package Excel from Audited Weight View", uid);

        String filename = "packages.xls";
        List<Package> packages = packageDao.search(request, userDetails.getCompany().getCode());
        byte[] excel = ExcelHelper.packagesToExcel(packages);


        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", "application/vnd.ms-excel;");
        headers.set("content-length",Integer.toString(excel.length));
        headers.set("Content-Disposition", "attachment; filename=" + filename);

        logger.info("{} - Finished downloading Package Excel from Audited Weight View", uid);

        return new ResponseEntity<>(excel, headers, HttpStatus.CREATED);

    }


    @PostMapping(value = "/upload")
    public String fileUpload(@RequestParam("file") MultipartFile file, @ModelAttribute("hub") String selectedCompany, Principal principal) {

        String uid = principal.getName();

        logger.info("{} - Started processing Package file.", uid);

        if (file.isEmpty()) {
            logger.info("{} - Package file was empty.", uid);
            return "redirect:/weight";
        }

        try {

            logger.info("{} - Started Analyzing Package file.", uid);
            HSSFWorkbook wb  = new HSSFWorkbook(file.getInputStream());
            List<Package> packages = ExcelHelper.readPackages(wb, selectedCompany);
            logger.info("{} - Finished Analyzing Package file. There were a total of: " + packages.size() + " Packages.", uid);

            if(packages != null && !packages.isEmpty()){
                //update DB
                logger.info("{} - Started writing packages in DB.", uid);
                int batchSize = properties.getUploadBatchSize();
                packageDao.batchUpdate(packages, batchSize);
                logger.info("{} - Finished writing packages in DB.", uid);
            }

        } catch (Exception e) {
            logger.error("{} - Error processing Package file.", uid);
            return "redirect:/weight";
        }

        return "redirect:/weight";
    }

    @PostMapping(value = "/add")
    public String addWeight(@RequestParam("image") MultipartFile file,
                            @RequestParam("hub_add") String hubAdd,
                            @RequestParam("tracking_number") String trackingNumber,
                            @RequestParam("audited_length") String auditedLength,
                            @RequestParam("audited_width") String auditedWidth,
                            @RequestParam("audited_height") String auditedHeight,
                            @RequestParam("audited_weight") String auditedWeight,
                            @RequestParam("audited_volumetric_weight") String auditedVolumetricWeight,
                            @RequestParam("chargeable_weight") String chargeableWeight,
                            @RequestParam("date_add") String date,
                            Principal principal) {

        String uid = principal.getName();

        Package newPackage = new Package();
        newPackage.setHub(hubAdd);
        newPackage.setTrackingNumber(trackingNumber);

        newPackage.setAuditedWidth(new BigDecimal(auditedWidth).setScale(2, RoundingMode.UP));
        newPackage.setAuditedWeight(new BigDecimal(auditedWeight).setScale(2, RoundingMode.UP));
        newPackage.setAuditedLength(new BigDecimal(auditedLength).setScale(2, RoundingMode.UP));
        newPackage.setAuditedHeight(new BigDecimal(auditedHeight).setScale(2, RoundingMode.UP));

        if(StringUtils.isEmpty(chargeableWeight)){
            PackageHelper.updateChargeableWeight(newPackage);
        } else {
            newPackage.setChargeableWeight(new BigDecimal(chargeableWeight).setScale(1, RoundingMode.UP));
        }

        if(StringUtils.isEmpty(auditedVolumetricWeight)){
            PackageHelper.updateVolumetricWeight(newPackage);
        } else {
            newPackage.setAuditedVolumetricWeight(new BigDecimal(auditedVolumetricWeight).setScale(2, RoundingMode.UP));
        }


        LocalDate localDate = LocalDate.parse(date);
        LocalDateTime dateTime = LocalDateTime.of(localDate, LocalTime.now());
        newPackage.setDateTime(dateTime);
        newPackage.setStatus(null);
        if(StringUtils.isNotEmpty(properties.getImageUploadPath())) {
                String destinationUrl = ImageHelper.writeFile(properties.getImageUploadPath(), newPackage, file);
                List<ImageInfo> imageInfos = List.of(new ImageInfo(DEFAULT_STATUS, destinationUrl));
                newPackage.setImageInfos(imageInfos);
            }


        String insertedTrackingNo = packageDao.insert(newPackage);

        return "redirect:/weight";
    }

}
