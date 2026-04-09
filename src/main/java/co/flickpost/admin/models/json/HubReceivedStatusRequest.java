package co.flickpost.admin.models.json;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HubReceivedStatusRequest {
    private String dateTime;
    private String location;
    private String shipmentMainStatus;
    private String shipmentParcelStatus;
    private String note;
    private List<String> trackingNumbers;
}
