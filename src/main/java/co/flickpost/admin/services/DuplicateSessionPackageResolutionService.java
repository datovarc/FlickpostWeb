package co.flickpost.admin.services;

import co.flickpost.admin.models.SessionPackage;
import co.flickpost.admin.models.json.DuplicateResolutionRequest;
import co.flickpost.admin.models.json.PendingDuplicateSessionPackage;
import co.flickpost.admin.repositories.SessionPackageDao;
import org.apache.commons.lang3.StringUtils;
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

    @Autowired
    private ScanSessionService scanSessionService;

    @Transactional
    public Map<String, Object> resolve(DuplicateResolutionRequest request) {
        if (request == null || request.getTrackingNumber() == null || request.getSelectedRecord() == null) {
            throw new IllegalArgumentException("trackingNumber and selectedRecord are required");
        }

        PendingDuplicateSessionPackage pending = pendingDuplicateSessionPackageService.remove(request.getTrackingNumber());
        if (pending == null) {
            throw new IllegalArgumentException("No pending duplicate found for trackingNumber: " + request.getTrackingNumber());
        }

        String duplicateType = pending.getDuplicateType();
        SessionPackage newRecord = pending.getNewRecord();
        String selectedRecord = request.getSelectedRecord();
        String remark = StringUtils.trimToNull(request.getRemark());

        if ("session-package".equalsIgnoreCase(duplicateType)) {
            resolveSessionPackageDuplicate(request.getTrackingNumber(), selectedRecord, newRecord);
        } else if ("package".equalsIgnoreCase(duplicateType)) {
            resolvePackageDuplicate(request.getTrackingNumber(), selectedRecord, newRecord, remark);
        } else {
            throw new IllegalArgumentException("Unsupported duplicate type: " + duplicateType);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("trackingNumber", request.getTrackingNumber());
        response.put("selectedRecord", request.getSelectedRecord());
        response.put("duplicateType", duplicateType);
        return response;
    }

    private void resolveSessionPackageDuplicate(String trackingNumber, String selectedRecord, SessionPackage newRecord) {
        if ("new".equalsIgnoreCase(selectedRecord)) {
            SessionPackage oldRecord = sessionPackageDao.findSessionPackageByTrackingNumber(trackingNumber);
            if (oldRecord != null) {
                sessionPackageDao.delete(oldRecord);
            }
            sessionPackageDao.insert(newRecord);
            scanSessionSseService.publishPackageIngested(trackingNumber);
            return;
        }

        if ("old".equalsIgnoreCase(selectedRecord)) {
            scanSessionSseService.publishPackageIngested(trackingNumber);
            return;
        }

        throw new IllegalArgumentException("selectedRecord must be 'old' or 'new' for session-package duplicates");
    }

    private void resolvePackageDuplicate(String trackingNumber, String selectedRecord, SessionPackage newRecord, String remark) {
        if ("old".equalsIgnoreCase(selectedRecord)) {
            scanSessionSseService.publishPackageIngested(trackingNumber);
            return;
        }

        if ("new".equalsIgnoreCase(selectedRecord) || "both".equalsIgnoreCase(selectedRecord)) {
            newRecord.setDuplicateRibbon(true);
            newRecord.setDuplicateRemark(remark);
            sessionPackageDao.insert(newRecord);
            scanSessionService.incrementActiveSessionTotalPackages();
            scanSessionSseService.publishPackageIngested(trackingNumber);
            return;
        }

        throw new IllegalArgumentException("selectedRecord must be 'old', 'new' or 'both' for package duplicates");
    }
}
