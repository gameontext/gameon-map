package net.wasdev.gameon.map.models;

import io.swagger.annotations.ApiModelProperty;

/**
 * API parameters for room/site swap
 *
 */
public class SiteSwap {

    @ApiModelProperty(
            value = "Site 1",
            required = true)
    private SiteCoordinates site1;

    @ApiModelProperty(
            value = "Site 2",
            required = true)
    private SiteCoordinates site2;

    public SiteSwap() {};

    public SiteSwap(SiteCoordinates site1, SiteCoordinates site2) {
        this.site1 = site1;
        this.site2 = site2;
    }

    public SiteCoordinates getSite1() {
        return site1;
    }

    public void setSite1(SiteCoordinates site1) {
        this.site1 = site1;
    }

    public SiteCoordinates getSite2() {
        return site2;
    }

    public void setSite2(SiteCoordinates site2) {
        this.site2 = site2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((site1 == null) ? 0 : site1.hashCode());
        result = prime * result + ((site2 == null) ? 0 : site2.hashCode());
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
        SiteSwap other = (SiteSwap) obj;
        if (site1 == null) {
            if (other.site1 != null)
                return false;
        } else if (!site1.equals(other.site1))
            return false;
        if (site2 == null) {
            if (other.site2 != null)
                return false;
        } else if (!site2.equals(other.site2))
            return false;
        return true;
    }
}
