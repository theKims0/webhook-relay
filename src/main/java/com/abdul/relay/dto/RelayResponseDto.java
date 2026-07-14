package com.abdul.relay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelayResponseDto {
    private String messageId;
    private int    statusCode;
    private String headers;
    private String body;
}
