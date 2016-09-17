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

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.ektorp.support.TypeDiscriminator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * When rooms or suites are added, they are persisted into the data store
 * as suites.
 *
 * Not all elements that are stored in the datastore are returned to the user.
 *
 */
@ApiModel(
        description = "A room (or suite) is anchored into the map as a site when it is registered. "
                + "The mapping should remain fairly stable unless a room is removed and re-appears.")
@JsonInclude(Include.NON_EMPTY)
public class Site {

    /** Site id */
    @JsonProperty("_id")
    @ApiModelProperty(
            value = "Site id",
            readOnly = true,
            name = "_id",
            example = "1",
            required = true)
    private String id;

    /** Document revision */
    @JsonProperty("_rev")
    @ApiModelProperty(hidden = true)
    private String rev;

    /** Descriptive room info */
    @ApiModelProperty(required = true)
    private RoomInfo info;

    /** Exit bindings: the other rooms' doors */
    @ApiModelProperty(required = true)
    private Exits exits;

    /** Owner of the room */
    @ApiModelProperty(
            value = "Owner",
            required = true)
    private String owner;

    /** Date site assigned */
    @ApiModelProperty(
            value="Date site created in ISO-8601 instant format",
            example = "2011-12-03T10:15:30Z",
            name = "created_on",
            required = false)
    private String createdOn;

    /** Date site assigned */
    @ApiModelProperty(value="Date room assigned in ISO-8601 instant format",
            example = "2011-12-03T10:15:30Z",
            name = "assigned_on",
            required = false)
    private String assignedOn;

    @TypeDiscriminator
    private Coordinates coord;

    @ApiModelProperty(hidden = true)
    private String type;

    public Site() {
        this.createdOn = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    public Site(int x, int y) {
        type = "placeholder";
        coord = new Coordinates(x, y);
        this.createdOn = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

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

    public String getOwner() {
        return owner;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }

    public RoomInfo getInfo() {
        return info;
    }
    public void setInfo(RoomInfo info) {
        this.info = info;
        this.assignedOn = info == null ? null : DateTimeFormatter.ISO_INSTANT.format(Instant.now());
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

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    @JsonIgnore
    public Instant getCreatedInstant() {
        if ( createdOn == null )
            return null;

        return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(createdOn));
    }

    public String getAssignedOn() {
        return assignedOn;
    }

    public void setAssignedOn(String assignedOn) {
        this.assignedOn = assignedOn;
    }

    @JsonIgnore
    public Instant getAssignedInstant() {
        if ( assignedOn == null )
            return null;

        return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(assignedOn));
    }

    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("  _id: \"").append(id).append("\",\n");
      sb.append("  _rev: \"").append(rev).append("\",\n");
      sb.append("  createdOn: \"").append(createdOn).append("\",\n");
      sb.append("  assignedOn: \"").append(assignedOn).append("\",\n");
      sb.append("  type: \"").append(type).append("\",\n");
      sb.append("  coord: ").append(coord).append(",\n");
      sb.append("  info: ").append(info).append(",\n");
      sb.append("  exits: ").append(exits).append("\n");
      sb.append("}");
      return sb.toString();
    }

}