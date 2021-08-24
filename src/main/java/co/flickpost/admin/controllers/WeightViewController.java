package co.flickpost.admin.controllers;


import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.helpers.ExcelHelper;
import co.flickpost.admin.models.User;
import co.flickpost.admin.models.json.DownloadRequest;
import co.flickpost.admin.models.Package;
import co.flickpost.admin.models.json.PaginationRequest;
import co.flickpost.admin.models.json.PaginationResponse;
import co.flickpost.admin.repositories.PackageDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/weight")
public class WeightViewController {

    @Autowired
    PackageDao packageDao;
    @Autowired
    FlickPostProperties properties;
    @Autowired
    BCryptPasswordEncoder encoder;

    private static final Logger logger = LogManager.getLogger(WeightViewController.class);

    @GetMapping(value = "")
    public String weightView() {
        return "weight";
    }


    @PostMapping(value = "/data")
    @ResponseBody
    public PaginationResponse weightPagination(@RequestBody PaginationRequest request) {
        Map<String, Object> packagesPagination = packageDao.pagination(request);
        if(packagesPagination == null){
            return new PaginationResponse(0, new ArrayList());
        }
        Long count = (Long) packagesPagination.get("count");
        List<Package> packages = (List) packagesPagination.get("packages");
        processImages(packages);

        int numPages = count.intValue() / request.getPageSize() + ((count.intValue() % request.getPageSize() == 0) ? 0 : 1);

        PaginationResponse paginationResponse = new PaginationResponse<>(numPages, packages);

        return paginationResponse;
    }

    @PostMapping(value = "/download")
    @ResponseBody
    public ResponseEntity<byte[]> downloadWeight(@RequestBody DownloadRequest request) throws IOException {
        String filename = "packages.xls";
        List<Package> packages = packageDao.search(request);
        byte[] excel = ExcelHelper.packagesToExcel(packages);


        HttpHeaders headers = new HttpHeaders();

        headers.set("Content-Type", "application/vnd.ms-excel;");
        headers.set("content-length",Integer.toString(excel.length));
        headers.set("Content-Disposition", "attachment; filename=" + filename);

        return new ResponseEntity<>(excel, headers, HttpStatus.CREATED);

    }


    @PostMapping(value = "/upload")
    public String fileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:/weight";
        }

        try {

            HSSFWorkbook wb  = new HSSFWorkbook(file.getInputStream());
            List<Package> packages = ExcelHelper.readPackages(wb);

            if(packages != null && !packages.isEmpty()){
                //update DB
                int batchSize = properties.getUploadBatchSize();
                packageDao.batchUpdate(packages, batchSize);
            }

        } catch (Exception e) {
            logger.error("Error processing file.", e);
            redirectAttributes.addFlashAttribute("message",
                    "Error processing file.\nPlease check format and try again.");
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
