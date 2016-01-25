package org.gameon.map.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Coordinates {
    int x;
    int y;

    @JsonIgnore
    public void setCoords(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class Coordinates {\n");
      sb.append("  x: ").append(x).append("\n");
      sb.append("  y: ").append(y).append("\n");
      sb.append("}\n");
      return sb.toString();
    }
}
