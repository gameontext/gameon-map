package org.gameontext.map.models;

import io.swagger.annotations.ApiModelProperty;

public class SiteCoordinates {
    @ApiModelProperty(
            value = "Site id",
            readOnly = true,
            example = "1",
            required = true)
    private String id;

    @ApiModelProperty(
            value = "Site coordinates",
            readOnly = true,
            required = true)
    private Coordinates coord;

    public SiteCoordinates() {};

    public SiteCoordinates(String id, Coordinates coord) {
        this.id = id;
        this.coord = coord;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Coordinates getCoord() {
        return coord;
    }

    public void setCoord(Coordinates coord) {
        this.coord = coord;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((coord == null) ? 0 : coord.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SiteCoordinates other = (SiteCoordinates) obj;
        if (coord == null) {
            if (other.coord != null)
                return false;
        } else if (!coord.equals(other.coord))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
