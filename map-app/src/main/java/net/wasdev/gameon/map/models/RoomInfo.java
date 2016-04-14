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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(
        description = "Mutable information: descriptive elements, service URL, etc.")
@JsonInclude(Include.NON_EMPTY)
public class RoomInfo {
    
    /** name of room (short / url-friendly) */
    private String name;

    /** Connection details */
    private ConnectionDetails connectionDetails = null;

    /** full name */
    private String fullName;

    /** Room door */
    private String description;

    /** Optional door descriptions */
    private Doors doors;
    
    @ApiModelProperty(
            value = "Short/Terse name of the target room, must be unique within the owner's rooms",
            example = "First Room",
            required = true)
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(required = true)
    public ConnectionDetails getConnectionDetails() {
        return connectionDetails;
    }
    public void setConnectionDetails(ConnectionDetails connectionDetails) {
        this.connectionDetails = connectionDetails;
    }

    @ApiModelProperty(
            value = "Human-friendly room name",
            example = "The First Room",
            required = false)
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @ApiModelProperty(
            value = "Player-friendly room description (140 characters)",
            example = "A helpful room with doors in every possible direction.",
            required = false)
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(
            required = false)
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
      sb.append("  description: ").append(description).append("\n");
      sb.append("  doors: ").append(doors).append("\n");
      sb.append("  connectionDetails: ").append(connectionDetails).append("\n");
      sb.append("}\n");
      return sb.toString();
    }
}