package co.flickpost.admin.models.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeightProcessResponse {
    @JsonProperty("code")
    private Integer code;

    @JsonProperty("error")
    private String error;
}
