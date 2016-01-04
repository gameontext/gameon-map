package org.gameon.map;

/**
 * Room-centric view of an established path. 
 */
@io.swagger.annotations.ApiModel(
        description = "When a room is placed in the map, paths are created between it and other rooms. "
                + "Each exit provides the door and URL required for the player to traverse the path "
                + "to the target room.")
public class Exits {
    
    private Exit n = null;
    private Exit w = null;
    private Exit s = null;
    private Exit e = null;
    private Exit u = null;
    private Exit d = null;

    @io.swagger.annotations.ApiModelProperty(
            value = "Only present if there is an established path North",
            required = false)
    public Exit getN() {
        return n;
    }

    public void setN(Exit n) {
        this.n = n;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Only present if there is an established path South",
            required = false)
    public Exit getS() {
        return s;
    }

    public void setS(Exit s) {
        this.s = s;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Only present if there is an established path East",
            required = false)
    public Exit getE() {
        return e;
    }

    public void setE(Exit e) {
        this.e = e;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Only present if there is an established path West",
            required = false)
    public Exit getW() {
        return w;
    }

    public void setW(Exit w) {
        this.w = w;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Only present if there is an established path Up",
            required = false)
    public Exit getU() {
        return u;
    }

    public void setU(Exit u) {
        this.u = u;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Only present if there is an established path Down",
            required = false)
    public Exit getD() {
        return d;
    }

    public void setD(Exit d) {
        this.d = d;
    }
    

    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class Exits {\n");
      sb.append("  n: ").append(n).append("\n");
      sb.append("  w: ").append(w).append("\n");
      sb.append("  s: ").append(s).append("\n");
      sb.append("  e: ").append(e).append("\n");
      sb.append("  u: ").append(u).append("\n");
      sb.append("  d: ").append(d).append("\n");
      sb.append("}\n");
      return sb.toString();
    }
}
