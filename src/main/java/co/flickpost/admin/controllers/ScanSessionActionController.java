package co.flickpost.admin.controllers;

import co.flickpost.admin.security.UserDetailsImpl;
import co.flickpost.admin.services.ScanSessionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/scan-session/session")
public class ScanSessionActionController {

    private static final Logger logger = LogManager.getLogger(ScanSessionActionController.class);

    @Autowired
    private ScanSessionService scanSessionService;

    @GetMapping("/status")
    public Map<String, Object> getStatus(org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        logger.info("{} - Received scan session status API request", userDetails.getUsername());
        return scanSessionService.getStatus(userDetails);
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        logger.info("{} - Received scan session start API request", userDetails.getUsername());
        return ResponseEntity.ok(scanSessionService.startSession(userDetails));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop(org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        logger.info("{} - Received scan session stop API request", userDetails.getUsername());
        return ResponseEntity.ok(scanSessionService.stopSession(userDetails));
    }
}
