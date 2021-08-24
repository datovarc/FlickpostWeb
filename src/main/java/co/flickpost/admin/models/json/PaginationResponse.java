package co.flickpost.admin.models.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaginationResponse<T> implements Serializable {


    @JsonProperty("last_page")
    private Integer lastPage;

    @JsonProperty("data")
    private List<T> data;

    public PaginationResponse(){}

    public PaginationResponse(Integer lastPage, List<T> data){
        this.lastPage = lastPage;
        this.data = data;
    }


    public Integer getLastPage() {
        return lastPage;
    }

    public void setLastPage(Integer lastPage) {
        this.lastPage = lastPage;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
