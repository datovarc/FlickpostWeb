package co.flickpost.admin.controllers;

import co.flickpost.admin.models.ScanSession;
import co.flickpost.admin.models.User;
import co.flickpost.admin.repositories.ScanSessionDao;
import co.flickpost.admin.security.UserDetailsImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/scan-session/session")
public class ScanSessionActionController {

    private static final Logger logger = LogManager.getLogger(ScanSessionActionController.class);
    private static final String ACTIVE = "ACTIVE";
    private static final String FINISHED = "FINISHED";

    @Autowired
    private ScanSessionDao scanSessionDao;

    @GetMapping("/status")
    @Transactional
    public Map<String, Object> getStatus(org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return buildStatusResponse(userDetails.getUsername());
    }

    @PostMapping("/toggle")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggle(org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String username = userDetails.getUsername();

        Optional<ScanSession> activeSessionOptional = scanSessionDao.findFirstByStatusOrderByStartTimeDesc(ACTIVE);
        if (activeSessionOptional.isPresent()) {
            ScanSession activeSession = activeSessionOptional.get();
            activeSession.setEndTime(LocalDateTime.now());
            activeSession.setStatus(FINISHED);
            scanSessionDao.save(activeSession);
            logger.info("{} - Finished scan session {}", username, activeSession.getId());
            return ResponseEntity.ok(buildStatusResponse(username));
        }

        ScanSession newSession = new ScanSession();
        newSession.setStartTime(LocalDateTime.now());
        newSession.setEndTime(null);
        newSession.setStatus(ACTIVE);
        newSession.setTotalPackages(0);
        newSession.setStaffName(resolveStaffName(userDetails));
        ScanSession savedSession = scanSessionDao.save(newSession);
        logger.info("{} - Started scan session {}", username, savedSession.getId());
        return ResponseEntity.ok(buildStatusResponse(username));
    }

    private Map<String, Object> buildStatusResponse(String username) {
        Optional<ScanSession> activeSessionOptional = scanSessionDao.findFirstByStatusOrderByStartTimeDesc(ACTIVE);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("active", activeSessionOptional.isPresent());
        response.put("buttonLabel", activeSessionOptional.isPresent() ? "Stop" : "Start");
        response.put("iconClass", activeSessionOptional.isPresent() ? "ti-control-stop" : "ti-control-play");
        response.put("shouldRedirectToScanSession", activeSessionOptional.isPresent());
        activeSessionOptional.ifPresent(scanSession -> {
            response.put("sessionId", scanSession.getId());
            response.put("staffName", scanSession.getStaffName());
            response.put("status", scanSession.getStatus());
        });
        logger.info("{} - Scan session active status checked: {}", username, activeSessionOptional.isPresent());
        return response;
    }

    private String resolveStaffName(UserDetailsImpl userDetails) {
        User user = userDetails.getUser();
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            return user.getFullName().trim();
        }
        if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            return user.getNickname().trim();
        }
        return user.getUsername();
    }
}
