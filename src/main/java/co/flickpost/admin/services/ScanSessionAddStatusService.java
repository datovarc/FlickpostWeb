package co.flickpost.admin.services;

import co.flickpost.admin.models.json.AddStatusSaveRequest;
import co.flickpost.admin.models.json.AddStatusSaveResponse;
import co.flickpost.admin.models.json.AuditedValuesByTrackingPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScanSessionAddStatusService {

    @Autowired
    private HubReceivedStatusService hubReceivedStatusService;

    @Autowired
    private AuditedValuesByTrackingService auditedValuesByTrackingService;

    public AddStatusSaveResponse save(AddStatusSaveRequest request) {
        if (request == null || request.getPackages() == null || request.getPackages().isEmpty()) {
            throw new IllegalArgumentException("No packages to process");
        }

        List<String> trackingNumbers = new ArrayList<>();
        for (AuditedValuesByTrackingPayload payload : request.getPackages()) {
            if (payload != null && payload.getTrackingNumber() != null && !payload.getTrackingNumber().trim().isEmpty()) {
                trackingNumbers.add(payload.getTrackingNumber().trim());
            }
        }

        boolean statusUpdated = hubReceivedStatusService.sendHubReceivedStatus(trackingNumbers).isSuccess();
        boolean coreSystemUpdated = auditedValuesByTrackingService.sendAuditedValues(request.getPackages());

        if (statusUpdated && coreSystemUpdated) {
            return new AddStatusSaveResponse(true, true, true, "Updates were successful");
        }
        if (!statusUpdated && !coreSystemUpdated) {
            return new AddStatusSaveResponse(false, false, false, "Update status and Update core system failed");
        }
        if (!statusUpdated) {
            return new AddStatusSaveResponse(false, false, true, "Update status failed");
        }
        return new AddStatusSaveResponse(false, true, false, "Update core system failed");
    }
}
