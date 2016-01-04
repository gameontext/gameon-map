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
package org.gameon.map;

@io.swagger.annotations.ApiModel(
        description = "A room is anchored into the map when it is registered. "
                + "Assigned paths between rooms will persist until something "
                + "like the deletion of a room requires them to change.")
public class MappedRoom {

    /** Room id */
    @io.swagger.annotations.ApiModelProperty(
            value = "Mapped room id",
            readOnly = true, 
            example = "1",
            required = true)
    private int id;

    /** Descriptive room info */
    @io.swagger.annotations.ApiModelProperty(
            value = "Information about the room: descriptive elements, service URL, etc.")
    private RoomInfo info;
    
    /** Exit bindings: where the room is in the map */
    @io.swagger.annotations.ApiModelProperty(
            value = "Information about the room: descriptive elements, service URL, etc.")
    private Exits exits;
    
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
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
    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class MappedRoom {\n");
      
      sb.append("  id: ").append(id).append("\n");
      sb.append("  info: ").append(info).append("\n");
      sb.append("  exits: ").append(exits).append("\n");
      sb.append("}\n");
      return sb.toString();
    }
   
}