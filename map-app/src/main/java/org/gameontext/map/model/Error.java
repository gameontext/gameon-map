package org.gameontext.map.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(
        description = "Error responses contain the HTTP status code, a message describing the error. Additional details may be provided.")
@JsonInclude(Include.NON_EMPTY)
public class Error {

    @ApiModelProperty(value = "Status code", example = "409", required = true)
    private int code;

    @ApiModelProperty(value = "Error message", example = "Conflict", required = true)
    private String message;

    @ApiModelProperty(value = "Additional information to help resolve error", example = "Conflict", required = false)
    @JsonProperty("more_info")
    private String moreInfo;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
