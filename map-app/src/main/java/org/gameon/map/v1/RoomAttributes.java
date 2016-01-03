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
package org.gameon.map.v1;

@io.swagger.annotations.ApiModel
public class RoomAttributes {

    @io.swagger.annotations.ApiModelProperty(
            value = "URL-friendly room name",
            example = "FirstRoom",
            required = true)
    String name;
    
    @io.swagger.annotations.ApiModelProperty(
            value = "Human-friendly room name",
            example = "The First Room",
            required = false)
    String longName;
    
    @io.swagger.annotations.ApiModelProperty(
            value = "Player-friendly description that fits in a tweet (140 characters)",
            example = "A helpful room with doors in every possible direction.",
            required = false)
    String description;
    
    @io.swagger.annotations.ApiModelProperty(
            value = "Descriptions for the doors in the room. ",
            required = false)
    RoomDoors doors;
        
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLongName() {
        return longName;
    }
    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    public RoomDoors getDoors() {
        return doors;
    }
    public void setDoors(RoomDoors doors) {
        this.doors = doors;
    }
}