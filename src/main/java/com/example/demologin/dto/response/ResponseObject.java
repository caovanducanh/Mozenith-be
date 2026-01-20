package com.example.demologin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResponseObject {
    @JsonProperty("statusCode")
    private int statusCode; // Thêm trạng thái HTTP

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data;
    

}
