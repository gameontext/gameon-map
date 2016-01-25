package org.gameon.map.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Connection details used by the mediator to connect to the room on the player's behalf")
@JsonInclude(Include.NON_EMPTY)
public class ConnectionDetails {

    String type;

    String target;

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

    @Override
    public String toString()  {
      StringBuilder sb = new StringBuilder();
      sb.append("class ConnectionDetails {\n");
      sb.append("  type: ").append(type).append("\n");
      sb.append("  target: ").append(target).append("\n");
      sb.append("}\n");
      return sb.toString();
    }
}
