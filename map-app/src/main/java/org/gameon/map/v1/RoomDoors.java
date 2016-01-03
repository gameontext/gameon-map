package org.gameon.map.v1;

/**
 * Descriptions for the doors in the room. These may be used by other rooms to describe the appearance of the door. 
 * Keys should be relative to this room (e, w, n, s, u, d). Values are simple string descriptions for the outside 
 * of the door. If the string value references a direction at all, it should be the opposite direction:for the 
 * East door, the direction mentioned should be West. Descriptions are optional, and will be generated if absent. 
 */
@io.swagger.annotations.ApiModel(
        description = "Descriptions for the doors in the room. These may be used by other rooms "
                + "to describe the appearance of the door. Keys should be relative to this "
                + "room (e, w, n, s, u, d). Values are simple string descriptions for the "
                + "outside of the door. If the string value references a direction at all, "
                + "it should be the opposite direction:for the East door, the direction "
                + "mentioned should be West. Descriptions are optional, and will be generated "
                + "if absent.")
public class RoomDoors {
    
    private String n = null;
    private String w = null;
    private String s = null;
    private String e = null;
    private String u = null;
    private String d = null;

    @io.swagger.annotations.ApiModelProperty(
            value = "Player-friendly description of the north door that fits in a tweet (140 characters)",
            example = "A knobbly wooden door with a rough carving or a friendly face",
            required = false)
    public String getN() {
        return n;
    }

    public void setN(String n) {
        this.n = n;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Player-friendly description of the south door that fits in a tweet (140 characters)",
            example = "A warped wooden door with a friendly face branded on the corner",
            required = false)
    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Player-friendly description of the east door that fits in a tweet (140 characters)",
            example = "A polished wooden door with an inlaid friendly face",
            required = false)
    public String getE() {
        return e;
    }

    public void setE(String e) {
        this.e = e;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Player-friendly description of the west door that fits in a tweet (140 characters)",
            example = "A fake wooden door with stickers of friendly faces plastered all over it",
            required = false)
    public String getW() {
        return w;
    }

    public void setW(String w) {
        this.w = w;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Player-friendly description of the door in the ceiling that fits in a tweet (140 characters)",
            example = "A scuffed and scratched oaken trap door",
            required = false)
    public String getU() {
        return u;
    }

    public void setU(String u) {
        this.u = u;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Player-friendly description of the door in the floor that fits in a tweet (140 characters)",
            example = "A rough-cut particle board hatch",
            required = false)
    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }
    

    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class RoomDoors {\n");
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
