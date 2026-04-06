package co.flickpost.admin.models.json;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HubReceivedStatusResponse {
    private final boolean success;
    private final String message;
    private final int processedCount;
}
