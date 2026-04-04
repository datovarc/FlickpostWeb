package co.flickpost.admin.models.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionPackageEvaluationRequest {

    private Boolean isFilled;
    private String trackingNumber;
}
