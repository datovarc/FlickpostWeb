package co.flickpost.admin.models.json;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DuplicateResolutionRequest {
    private String trackingNumber;
    private String selectedRecord;
    private String remark;
}
