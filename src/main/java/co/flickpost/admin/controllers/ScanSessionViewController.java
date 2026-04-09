package co.flickpost.admin.controllers;

import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.models.Company;
import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.PaginationRequest;
import co.flickpost.admin.models.json.PaginationResponse;
import co.flickpost.admin.repositories.CompanyDao;
import co.flickpost.admin.repositories.SessionPackageDao;
import co.flickpost.admin.security.UserDetailsImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.transaction.Transactional;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/scan-session")
public class ScanSessionViewController {

    @Autowired
    SessionPackageDao sessionPackageDao;
    @Autowired
    CompanyDao companyDao;
    @Autowired
    FlickPostProperties flickPostProperties;

    private static final Logger logger = LogManager.getLogger(ScanSessionViewController.class);

    @GetMapping(value = "")
    @Transactional
    public String scanSessionView(Model model, org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String uid = userDetails.getUsername();
        logger.info("{} - Started fetching All companies for Scan Session", uid);

        List<Company> companies = companyDao.getAllCompanies();
        companies = companies.stream().filter(company -> !company.equals(userDetails.getCompany())).collect(Collectors.toList());

        model.addAttribute("companies", companies);
        model.addAttribute("defaultCompany", userDetails.getCompany());
        model.addAttribute("addStatusMainStatusOptions", flickPostProperties.getScanSessionAddStatusMainStatusOptionEntries());
        model.addAttribute("addStatusSubStatusOptionsByMainStatus", flickPostProperties.getScanSessionAddStatusSubStatusOptionEntriesByMainStatus());
        return "scan-session";
    }

    @PostMapping(value = "/data")
    @ResponseBody
    @Transactional
    public PaginationResponse<SessionPackage> scanSessionData(@RequestBody PaginationRequest request, org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String uid = userDetails.getUsername();

        logger.info("{} - Started fetching data for Scan Session View", uid);
        Map<String, Object> packagesPagination = sessionPackageDao.pagination(request, userDetails.getCompany().getCode());
        if (packagesPagination == null) {
            logger.info("{} - There was no data for Scan Session View", uid);
            return new PaginationResponse<>(0, Collections.emptyList());
        }

        Long count = (Long) packagesPagination.get("count");
        List<SessionPackage> packages = (List<SessionPackage>) packagesPagination.get("packages");
        applyRounding(packages);

        logger.info("{} - There was a total of {} session packages for Scan Session View", uid, count);
        logger.info("{} - Currently displaying Page {} for Scan Session View", uid, request.getPageNumber());

        int numPages = count.intValue() / request.getPageSize() + ((count.intValue() % request.getPageSize() == 0) ? 0 : 1);
        logger.info("{} - Total of {} pages available for Scan Session View", uid, numPages);

        return new PaginationResponse<>(numPages, packages);
    }

    private void applyRounding(List<SessionPackage> packages) {
        for (SessionPackage pkg : packages) {
            if (pkg.getAuditedLength() != null) pkg.setAuditedLength(pkg.getAuditedLength().setScale(2, RoundingMode.UP));
            if (pkg.getAuditedWidth() != null) pkg.setAuditedWidth(pkg.getAuditedWidth().setScale(2, RoundingMode.UP));
            if (pkg.getAuditedHeight() != null) pkg.setAuditedHeight(pkg.getAuditedHeight().setScale(2, RoundingMode.UP));
            if (pkg.getAuditedActualWeight() != null) pkg.setAuditedActualWeight(pkg.getAuditedActualWeight().setScale(2, RoundingMode.UP));
            if (pkg.getAuditedVolumetricWeight() != null) pkg.setAuditedVolumetricWeight(pkg.getAuditedVolumetricWeight().setScale(2, RoundingMode.UP));
            if (pkg.getAuditedChargeableWeight() != null) pkg.setAuditedChargeableWeight(pkg.getAuditedChargeableWeight().setScale(1, RoundingMode.UP));
            if (pkg.getDeclaredLength() != null) pkg.setDeclaredLength(pkg.getDeclaredLength().setScale(2, RoundingMode.UP));
            if (pkg.getDeclaredWidth() != null) pkg.setDeclaredWidth(pkg.getDeclaredWidth().setScale(2, RoundingMode.UP));
            if (pkg.getDeclaredHeight() != null) pkg.setDeclaredHeight(pkg.getDeclaredHeight().setScale(2, RoundingMode.UP));
            if (pkg.getDeclaredActualWeight() != null) pkg.setDeclaredActualWeight(pkg.getDeclaredActualWeight().setScale(2, RoundingMode.UP));
            if (pkg.getClientPaidWeight() != null) pkg.setClientPaidWeight(pkg.getClientPaidWeight().setScale(2, RoundingMode.UP));
        }
    }
}
