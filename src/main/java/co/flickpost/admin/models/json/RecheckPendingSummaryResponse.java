package co.flickpost.admin.models.json;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecheckPendingSummaryResponse {
    private final boolean success;
    private final String context;
    private final int recheckedCount;
    private final int foundFromApiCount;
    private final int foundFromDbCount;
    private final int stillMissingCount;
    private final String message;
}
