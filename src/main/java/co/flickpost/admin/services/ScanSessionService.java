package co.flickpost.admin.services;

import co.flickpost.admin.models.ScanSession;
import co.flickpost.admin.models.User;
import co.flickpost.admin.repositories.ScanSessionDao;
import co.flickpost.admin.security.UserDetailsImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ScanSessionService {

    private static final Logger logger = LogManager.getLogger(ScanSessionService.class);
    private static final String ACTIVE = "ACTIVE";
    private static final String FINISHED = "FINISHED";

    @Autowired
    private ScanSessionDao scanSessionDao;

    @Autowired
    private ScanSessionSseService scanSessionSseService;

    @Transactional
    public Map<String, Object> getStatus(UserDetailsImpl userDetails) {
        return buildStatusResponse(userDetails.getUsername());
    }

    @Transactional
    public Map<String, Object> startSession(UserDetailsImpl userDetails) {
        String username = userDetails.getUsername();
        Optional<ScanSession> activeSessionOptional = scanSessionDao.findFirstByStatusOrderByStartTimeDesc(ACTIVE);

        if (activeSessionOptional.isPresent()) {
            logger.info("{} - Scan session start requested but active session {} already exists", username, activeSessionOptional.get().getId());
            return buildStatusResponse(username);
        }

        ScanSession newSession = new ScanSession();
        newSession.setStartTime(LocalDateTime.now());
        newSession.setEndTime(null);
        newSession.setStatus(ACTIVE);
        newSession.setTotalPackages(0);
        newSession.setStaffName(resolveStaffName(userDetails));
        ScanSession savedSession = scanSessionDao.save(newSession);
        logger.info("{} - Started scan session {}", username, savedSession.getId());
        return buildStatusResponse(username);
    }

    @Transactional
    public Map<String, Object> stopSession(UserDetailsImpl userDetails) {
        String username = userDetails.getUsername();
        Optional<ScanSession> activeSessionOptional = scanSessionDao.findFirstByStatusOrderByStartTimeDesc(ACTIVE);

        if (!activeSessionOptional.isPresent()) {
            logger.info("{} - Scan session stop requested but no active session exists", username);
            return buildStatusResponse(username);
        }

        ScanSession activeSession = activeSessionOptional.get();
        activeSession.setEndTime(LocalDateTime.now());
        activeSession.setStatus(FINISHED);
        scanSessionDao.save(activeSession);
        scanSessionSseService.publishSessionStopped();
        logger.info("{} - Finished scan session {}", username, activeSession.getId());
        return buildStatusResponse(username);
    }

    private Map<String, Object> buildStatusResponse(String username) {
        Optional<ScanSession> activeSessionOptional = scanSessionDao.findFirstByStatusOrderByStartTimeDesc(ACTIVE);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("active", activeSessionOptional.isPresent());
        response.put("buttonLabel", activeSessionOptional.isPresent() ? "Stop" : "Start");
        response.put("iconClass", activeSessionOptional.isPresent() ? "ti-control-stop" : "ti-control-play");
        response.put("shouldRedirectToScanSession", !activeSessionOptional.isPresent());
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
        if (user.getNickname() != null) {
            return user.getNickname().trim();
        }
        return "";
    }
}
