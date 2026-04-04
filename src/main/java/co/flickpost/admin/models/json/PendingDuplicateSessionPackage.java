package co.flickpost.admin.models.json;

import co.flickpost.admin.models.SessionPackage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PendingDuplicateSessionPackage {
    private String trackingNumber;
    private SessionPackage oldRecord;
    private SessionPackage newRecord;
}
