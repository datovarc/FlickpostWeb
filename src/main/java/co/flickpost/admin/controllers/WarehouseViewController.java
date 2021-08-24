package co.flickpost.admin.controllers;


import co.flickpost.admin.models.Package;
import co.flickpost.admin.repositories.PackageDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;


@Controller
@RequestMapping("/warehouse")
public class WarehouseViewController {

    @Autowired
    PackageDao packageDao;

    private static final Logger logger = LogManager.getLogger(WarehouseViewController.class);

    @GetMapping(value = "")
    public String warehouse(Model model, Principal principal) {
        String uid = principal.getName();

        logger.info("{} - Fetching most recent packages for WarehouseView.", uid);
        List<Package> packages = packageDao.getMostRecent(10);
        logger.info("{} - Obtained {} packages for WarehouseView.", uid, packages.size());
        model.addAttribute("packages", packages);
        return "warehouse";
    }

}
