package co.flickpost.admin.models.json;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddStatusSaveResponse {
    private final boolean success;
    private final boolean statusUpdated;
    private final boolean coreSystemUpdated;
    private final String message;
}
