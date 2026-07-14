package com.abdul.relay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RelayRequestDTO {
        private String messageId;
        private String method;
        private String path;
        private String headers;
        private String body;
        private String timestamp;
}
