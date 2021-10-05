package co.flickpost.admin.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageInfo implements Serializable {
    @JsonProperty("status")
    private String status;
    @JsonProperty("path")
    private String path;

    public ImageInfo(){}

    public ImageInfo(String status, String path){
        this.status = status;
        this.path = path;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageInfo imageInfo = (ImageInfo) o;
        return Objects.equals(status, imageInfo.status) &&
                Objects.equals(path, imageInfo.path);
    }

    @Override
    public int hashCode() {

        return Objects.hash(status, path);
    }
}
