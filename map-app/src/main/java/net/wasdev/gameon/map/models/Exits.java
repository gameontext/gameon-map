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
package net.wasdev.gameon.map.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Room-centric view of an established path.
 */
@io.swagger.annotations.ApiModel(
        description = "When a room is placed in the map, paths are created between it and other rooms. "
                + "Each exit provides the door and URL required for the player to traverse the path "
                + "to the target room.")
@JsonInclude(Include.NON_EMPTY)
public class Exits {

    private Exit n = null;
    private Exit w = null;
    private Exit s = null;
    private Exit e = null;
    private Exit u = null;
    private Exit d = null;

    @io.swagger.annotations.ApiModelProperty(
            required = false)
    public Exit getN() {
        return n;
    }

    public void setN(Exit n) {
        this.n = n;
    }

    @io.swagger.annotations.ApiModelProperty(
            required = false)
    public Exit getS() {
        return s;
    }

    public void setS(Exit s) {
        this.s = s;
    }

    @io.swagger.annotations.ApiModelProperty(
            required = false)
    public Exit getE() {
        return e;
    }

    public void setE(Exit e) {
        this.e = e;
    }

    @io.swagger.annotations.ApiModelProperty(
            required = false)
    public Exit getW() {
        return w;
    }

    public void setW(Exit w) {
        this.w = w;
    }

    @io.swagger.annotations.ApiModelProperty(
            required = false)
    public Exit getU() {
        return u;
    }

    public void setU(Exit u) {
        this.u = u;
    }

    @io.swagger.annotations.ApiModelProperty(
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
