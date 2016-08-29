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
package org.gameontext.map.models;

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
            value = "A token used for mutual identification between the room and the mediator during the initial handshake when the connection is established (optional)",
            example = "A-totally-arbitrary-really-long-string",
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConnectionDetails other = (ConnectionDetails) obj;
        if (target == null) {
            if (other.target != null) {
                return false;
            }
        } else if (!target.equals(other.target)) {
            return false;
        }
        if (token == null) {
            if (other.token != null) {
                return false;
            }
        } else if (!token.equals(other.token)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }
}
