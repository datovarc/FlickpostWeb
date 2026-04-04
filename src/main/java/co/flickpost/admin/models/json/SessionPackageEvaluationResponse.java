package co.flickpost.admin.models.json;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SessionPackageEvaluationResponse {
    private final boolean success;
    private final String trackingNumber;
    private final String finalStatus;
    private final String dataSource;
    private final boolean backfilledFromReference;
    private final String message;
}
