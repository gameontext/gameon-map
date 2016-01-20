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
package org.gameon.map.models;

import org.ektorp.support.TypeDiscriminator;

import com.fasterxml.jackson.annotation.JsonProperty;

@io.swagger.annotations.ApiModel(
        description = "A room (or suite) is anchored into the map when it is registered. "
                + "Assigned paths between rooms/suites will persist until something "
                + "like the deletion of a room requires them to change.")
public class Node {

    /** Node id */
    @JsonProperty("_id")
    @io.swagger.annotations.ApiModelProperty(
            value = "Mapped room id",
            readOnly = true,
            name = "_id",
            example = "1",
            required = true)
    private String id;

    /** Document revision */
    @JsonProperty("_rev")
    private String rev;

    /** Descriptive room info */
    @io.swagger.annotations.ApiModelProperty(
            value = "Information about the room or suite: descriptive elements, service URL, etc.")
    private RoomInfo info;

    /** Exit bindings: the other rooms' doors */

    @io.swagger.annotations.ApiModelProperty(
            value = "Exits: Doors to other rooms")
    private Exits exits;

    @TypeDiscriminator
    @io.swagger.annotations.ApiModelProperty(hidden = true)
    private Coordinates coord;

    @io.swagger.annotations.ApiModelProperty(hidden = true)
    private String type;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getRev() {
        return rev;
    }
    public void setRev(String rev) {
        this.rev = rev;
    }

    public RoomInfo getInfo() {
        return info;
    }
    public void setInfo(RoomInfo info) {
        this.info = info;
    }

    public Exits getExits() {
        return exits;
    }
    public void setExits(Exits exits) {
        this.exits = exits;
    }

    public Coordinates getCoord() {
        return coord;
    }
    public void setCoord(Coordinates coord) {
        this.coord = coord;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class Node {\n");

      sb.append("  id: ").append(id).append("\n");
      sb.append("  rev: ").append(rev).append("\n");
      sb.append("  type: ").append(type).append("\n");
      sb.append("  coord: ").append(coord).append("\n");
      sb.append("  info: ").append(info).append("\n");
      sb.append("  exits: ").append(exits).append("\n");
      sb.append("}\n");
      return sb.toString();
    }

}