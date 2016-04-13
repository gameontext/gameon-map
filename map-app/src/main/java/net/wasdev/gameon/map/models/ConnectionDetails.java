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

@ApiModel(description = "Connection details used by the mediator to connect to the room on the player's behalf")
@JsonInclude(Include.NON_EMPTY)
public class ConnectionDetails {

    private String type;

    private String target;
    
    private String token;

    @ApiModelProperty(
            value = "Connection type",
            example = "websocket",
            required = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty(
            value = "Connection target, usually a URL",
            example = "wss://secondroom:9008/barn/ws",
            required = true)
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @ApiModelProperty(
            value = "A token which if supplied by the room is used to authenticate when the mediator establishes a connection.",
            example = "[any text string]",
            required = false)
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    
    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class ConnectionDetails {\n");
      sb.append("  type: ").append(type).append("\n");
      sb.append("  target: ").append(target).append("\n");
      sb.append("  token: ").append(token).append("\n");
      sb.append("}\n");
      return sb.toString();
    }
}
