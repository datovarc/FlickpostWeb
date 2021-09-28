package co.flickpost.admin.models.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DownloadRequest implements Serializable {


    @JsonFormat(pattern = "yyyy-MM-dd", timezone = JsonFormat.DEFAULT_TIMEZONE)
    @JsonProperty("from_date")
    private LocalDate fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = JsonFormat.DEFAULT_TIMEZONE)
    @JsonProperty("to_date")
    private LocalDate toDate;

    @JsonProperty("hub")
    private String hub;

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getHub() {
        return hub;
    }

    public void setHub(String hub) {
        this.hub = hub;
    }
}
