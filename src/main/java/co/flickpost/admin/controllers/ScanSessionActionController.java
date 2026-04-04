package co.flickpost.admin.controllers;

import co.flickpost.admin.configurations.FlickPostProperties;
import co.flickpost.admin.models.json.FpSessionPackageIngestRequest;
import co.flickpost.admin.models.json.FpSessionPackageIngestResponse;
import co.flickpost.admin.models.json.SessionPackageEvaluationRequest;
import co.flickpost.admin.models.json.SessionPackageEvaluationResponse;
import co.flickpost.admin.security.UserDetailsImpl;
import co.flickpost.admin.services.ScanSessionService;
import co.flickpost.admin.services.SessionPackageEvaluationService;
import co.flickpost.admin.services.SessionPackageIngestService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ScanSessionActionController {

    private static final Logger logger = LogManager.getLogger(ScanSessionActionController.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    @Autowired
    private ScanSessionService scanSessionService;

    @Autowired
    private SessionPackageEvaluationService sessionPackageEvaluationService;

    @Autowired
    private SessionPackageIngestService sessionPackageIngestService;

    @Autowired
    private FlickPostProperties flickPostProperties;

    @GetMapping("/scan-session/session/status")
    public Map<String, Object> getStatus(org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        logger.info("{} - Received scan session status API request", userDetails.getUsername());
        return scanSessionService.getStatus(userDetails);
    }

    @PostMapping("/scan-session/session/start")
    public ResponseEntity<Map<String, Object>> start(org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        logger.info("{} - Received scan session start API request", userDetails.getUsername());
        return ResponseEntity.ok(scanSessionService.startSession(userDetails));
    }

    @PostMapping("/scan-session/session/stop")
    public ResponseEntity<Map<String, Object>> stop(org.springframework.security.core.Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        logger.info("{} - Received scan session stop API request", userDetails.getUsername());
        return ResponseEntity.ok(scanSessionService.stopSession(userDetails));
    }

    @PostMapping("/api/scan-session/session/ingest-package")
    public ResponseEntity<?> ingestPackage(@RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
                                           @RequestBody FpSessionPackageIngestRequest request) {
        if (!flickPostProperties.getScanSessionEvaluationApiKey().equals(apiKey)) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Invalid API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        try {
            FpSessionPackageIngestResponse response = sessionPackageIngestService.ingest(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("trackingNumber", request != null ? request.getTrackingNumber() : null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping("/api/scan-session/session/evaluate-package")
    public ResponseEntity<?> evaluatePackage(@RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
                                             @RequestBody SessionPackageEvaluationRequest request) {
        if (!flickPostProperties.getScanSessionEvaluationApiKey().equals(apiKey)) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Invalid API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        logger.info("Received session package evaluation API request");

        try {
            SessionPackageEvaluationResponse response = sessionPackageEvaluationService.evaluate(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("trackingNumber", null);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
