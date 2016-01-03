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
public class RoomInfo {

    private int id;
    private RoomAttributes attributes;
    
    @io.swagger.annotations.ApiModelProperty(
            value = "Room id",
            readOnly = true, 
            example = "12345",
            required = true)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @io.swagger.annotations.ApiModelProperty(
            value = "Descriptive room attributes",
            example = "{}",
            required = true)
    public RoomAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(RoomAttributes attributes) {
        this.attributes = attributes;
    }
    
    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class RoomInfo {\n");
      
      sb.append("  id: ").append(id).append("\n");
      sb.append("  attributes: ").append(attributes).append("\n");
      sb.append("}\n");
      return sb.toString();
    }
   
}