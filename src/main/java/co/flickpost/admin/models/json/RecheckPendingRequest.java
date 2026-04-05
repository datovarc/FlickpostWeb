package co.flickpost.admin.models.json;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecheckPendingRequest {
    private String context;
    private List<String> trackingNumbers;
}
