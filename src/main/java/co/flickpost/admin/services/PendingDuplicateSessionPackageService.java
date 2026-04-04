package co.flickpost.admin.services;

import co.flickpost.admin.models.json.PendingDuplicateSessionPackage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PendingDuplicateSessionPackageService {

    private final Map<String, PendingDuplicateSessionPackage> pendingDuplicates = new ConcurrentHashMap<>();

    public void put(PendingDuplicateSessionPackage pendingDuplicate) {
        pendingDuplicates.put(pendingDuplicate.getTrackingNumber(), pendingDuplicate);
    }

    public PendingDuplicateSessionPackage get(String trackingNumber) {
        return pendingDuplicates.get(trackingNumber);
    }

    public PendingDuplicateSessionPackage remove(String trackingNumber) {
        return pendingDuplicates.remove(trackingNumber);
    }
}
