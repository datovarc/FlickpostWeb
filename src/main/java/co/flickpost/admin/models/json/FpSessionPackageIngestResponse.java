package co.flickpost.admin.models.json;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FpSessionPackageIngestResponse {
    private final boolean success;
    private final String trackingNumber;
    private final String finalStatus;
    private final String dataSource;
    private final Boolean isUnderdeclared;
    private final Boolean oversized;
    private final String message;
}
