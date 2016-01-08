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

@io.swagger.annotations.ApiModel(
        description = "Mutable information about a room: descriptive elements, service URL, etc.")
public class RoomInfo {

    /** name of room (short / url-friendly) */
    @io.swagger.annotations.ApiModelProperty(
            value = "Short name of the target room (small title bars)",
            example = "First Room",
            required = true)
    String name;
    
    /** full name */
    @io.swagger.annotations.ApiModelProperty(
            value = "Human-friendly room name",
            example = "The First Room",
            required = false)
    String fullName;
    
    /** Room door */
    @io.swagger.annotations.ApiModelProperty(
            value = "Player-friendly room description (140 characters)",
            example = "A helpful room with doors in every possible direction.",
            required = false)
    String description;
    
    /** Optional door descriptions */
    @io.swagger.annotations.ApiModelProperty(
            value = "Descriptions for the doors used to enter the room. ",
            required = false)
    Doors doors;
        
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Doors getDoors() {
        return doors;
    }
    public void setDoors(Doors doors) {
        this.doors = doors;
    }
    
    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class RoomInfo {\n");
      sb.append("  name: ").append(name).append("\n");
      sb.append("  fullName: ").append(fullName).append("\n");
      sb.append("  door: ").append(description).append("\n");
      sb.append("  doors: ").append(doors).append("\n");
      sb.append("}\n");
      return sb.toString();
    }
}