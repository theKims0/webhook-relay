package com.abdul.relay.service;

import com.abdul.relay.dto.RelayResponse;

import java.util.concurrent.TimeoutException;

public interface WebSocketRelayService {

    /**
     * Meneruskan request HTTP ke Relay Agent yang terhubung via WebSocket,
     * kemudian menunggu response dari agent (blocking, dengan timeout 30 detik).
     *
     * @param projectId UUID project (sebagai string)
     * @param method    HTTP method (GET, POST, PUT, DELETE, PATCH)
     * @param path      Path tujuan yang akan diteruskan ke service lokal (mis. /api/users)
     * @param headers   Header request dalam format JSON string
     * @param body      Request body sebagai string
     * @return RelayResponse berisi statusCode, headers, dan body dari target service
     * @throws TimeoutException jika agent tidak merespons dalam 30 detik
     * @throws IllegalStateException jika tidak ada agent yang terhubung untuk projectId tersebut
     */
    RelayResponse relay(String projectId, String method, String path, String headers, String body)
            throws TimeoutException, Exception;
}
