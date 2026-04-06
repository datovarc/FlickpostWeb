package co.flickpost.admin.models.json;

import co.flickpost.admin.models.Package;
import co.flickpost.admin.models.SessionPackage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PendingDuplicateSessionPackage {
    private String trackingNumber;
    private String duplicateType;
    private Object oldRecord;
    private SessionPackage newRecord;

    public static PendingDuplicateSessionPackage forSessionPackage(String trackingNumber, SessionPackage oldRecord, SessionPackage newRecord) {
        return new PendingDuplicateSessionPackage(trackingNumber, "session-package", oldRecord, newRecord);
    }

    public static PendingDuplicateSessionPackage forPackage(String trackingNumber, Package oldRecord, SessionPackage newRecord) {
        return new PendingDuplicateSessionPackage(trackingNumber, "package", oldRecord, newRecord);
    }
}
