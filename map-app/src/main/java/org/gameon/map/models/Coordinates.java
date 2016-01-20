package org.gameon.map.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Coordinates {
    int x;
    int y;
    int sort;

    @JsonIgnore
    public void setCoords(int x, int y) {
        this.x = x;
        this.y = y;
        this.sort = Math.abs(x) + Math.abs(y);
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

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class Coordinates {\n");
      sb.append("  x: ").append(x).append("\n");
      sb.append("  y: ").append(y).append("\n");
      sb.append("  sort: ").append(sort).append("\n");
      sb.append("}\n");
      return sb.toString();
    }
}
