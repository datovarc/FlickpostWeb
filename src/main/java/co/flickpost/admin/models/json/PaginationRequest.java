package co.flickpost.admin.models.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaginationRequest implements Serializable {


    @JsonProperty("page")
    private Integer pageNumber;

    @JsonProperty("size")
    private Integer pageSize;

    @JsonProperty("filters")
    private List<PaginationFilter> filters;

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<PaginationFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<PaginationFilter> filters) {
        this.filters = filters;
    }
}
