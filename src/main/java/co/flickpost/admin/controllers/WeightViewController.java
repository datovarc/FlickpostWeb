package co.flickpost.admin.controllers;


import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.helpers.ExcelHelper;
import co.flickpost.admin.helpers.PackageHelper;
import co.flickpost.admin.models.Company;
import co.flickpost.admin.models.json.DownloadRequest;
import co.flickpost.admin.models.Package;
import co.flickpost.admin.models.json.PaginationRequest;
import co.flickpost.admin.models.json.PaginationResponse;
import co.flickpost.admin.repositories.CompanyDao;
import co.flickpost.admin.repositories.PackageDao;
import co.flickpost.admin.security.UserDetailsImpl;
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
import java.security.Principal;
import java.util.Base64;
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
    public PaginationResponse weightPagination(@RequestBody PaginationRequest request, Principal principal) {
        String uid = principal.getName();

        logger.info("{} - Started fetching data for Audited Weight View", uid);
        Map<String, Object> packagesPagination = packageDao.pagination(request);
        if(packagesPagination == null){
            logger.info("{} - There was no data for Audited Weight View", uid);
            return new PaginationResponse(0, Collections.EMPTY_LIST);
        }

        Long count = (Long) packagesPagination.get("count");
        List<Package> packages = (List) packagesPagination.get("packages");
        PackageHelper.applyRounding(packages);

        logger.info("{} - There was a total of {} packages for Audited Weight View", uid, count);
        logger.info("{} - Currently displaying Page {} for Audited Weight View", uid, request.getPageNumber());

        logger.info("{} - Started processing images for Audited Weight View", uid);
        processImages(packages);
        logger.info("{} - Finished processing images for Audited Weight View", uid);

        int numPages = count.intValue() / request.getPageSize() + ((count.intValue() % request.getPageSize() == 0) ? 0 : 1);
        logger.info("{} - Total of {} pages available for Audited Weight View", uid, numPages);

        PaginationResponse paginationResponse = new PaginationResponse<>(numPages, packages);

        return paginationResponse;
    }

    @PostMapping(value = "/download")
    @ResponseBody
    public ResponseEntity<byte[]> downloadWeight(@RequestBody DownloadRequest request, Principal principal) throws IOException {
        String uid = principal.getName();

        logger.info("{} - Started downloading Package Excel from Audited Weight View", uid);

        String filename = "packages.xls";
        List<Package> packages = packageDao.search(request);
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


    private void processImages(List<Package> packages){
        for (Package pkg : packages) {
            if (pkg.getImage() != null) {
                String decodedPicture = Base64.getEncoder().encodeToString(pkg.getImage());
                pkg.setEncodedImage("data:image/jpg;base64," + decodedPicture);
            }
        }
    }

}
