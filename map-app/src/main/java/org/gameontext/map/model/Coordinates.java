/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameontext.map.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModelProperty;

public class Coordinates {

    @ApiModelProperty(
            value = "X coordinate",
            required = true)
    private int x;

    @ApiModelProperty(
            value = "Y coordinate",
            required = true)
    private int y;

    public Coordinates() {}

    @JsonIgnore
    public Coordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @JsonIgnore
    public Coordinates(Coordinates copy) {
        this.x = copy.x;
        this.y = copy.y;
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

    @JsonIgnore
    public void diagonalShift(int index) {
        x = x >= 0 ? x + index : x - index;
        y = y >= 0 ? y + index : y - index;
    }

    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append(" x: ").append(x).append(",");
      sb.append(" y: ").append(y).append(" ");
      sb.append("}");
      return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
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

        Coordinates other = (Coordinates) obj;
        return (x == other.x && y == other.y);
    }

    @JsonIgnore
    public boolean equals(int x, int y) {
        return this.x == x && this.y == y;
    }
}
