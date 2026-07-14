package com.abdul.relay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Merepresentasikan response yang diterima dari Relay Agent
 * dan akan dikembalikan ke caller asli.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelayResponse {
    /** HTTP status code dari target service lokal */
    private int statusCode;

    /** Response body dari target service lokal */
    private String body;

    /** Response headers dari target service lokal (JSON string) */
    private String headers;
}
