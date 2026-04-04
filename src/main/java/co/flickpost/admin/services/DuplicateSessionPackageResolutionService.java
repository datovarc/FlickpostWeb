package co.flickpost.admin.services;

import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.DuplicateResolutionRequest;
import co.flickpost.admin.models.json.PendingDuplicateSessionPackage;
import co.flickpost.admin.repositories.SessionPackageDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DuplicateSessionPackageResolutionService {

    @Autowired
    private PendingDuplicateSessionPackageService pendingDuplicateSessionPackageService;

    @Autowired
    private SessionPackageDao sessionPackageDao;

    @Autowired
    private ScanSessionSseService scanSessionSseService;

    @Transactional
    public Map<String, Object> resolve(DuplicateResolutionRequest request) {
        if (request == null || request.getTrackingNumber() == null || request.getSelectedRecord() == null) {
            throw new IllegalArgumentException("trackingNumber and selectedRecord are required");
        }

        PendingDuplicateSessionPackage pending = pendingDuplicateSessionPackageService.remove(request.getTrackingNumber());
        if (pending == null) {
            throw new IllegalArgumentException("No pending duplicate found for trackingNumber: " + request.getTrackingNumber());
        }

        if ("new".equalsIgnoreCase(request.getSelectedRecord())) {
            SessionPackage oldRecord = sessionPackageDao.findSessionPackageByTrackingNumber(request.getTrackingNumber());
            if (oldRecord != null) {
                sessionPackageDao.delete(oldRecord);
            }
            sessionPackageDao.insert(pending.getNewRecord());
            scanSessionSseService.publishPackageIngested(request.getTrackingNumber());
        } else if ("old".equalsIgnoreCase(request.getSelectedRecord())) {
            scanSessionSseService.publishPackageIngested(request.getTrackingNumber());
        } else {
            throw new IllegalArgumentException("selectedRecord must be 'old' or 'new'");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("trackingNumber", request.getTrackingNumber());
        response.put("selectedRecord", request.getSelectedRecord());
        return response;
    }
}
