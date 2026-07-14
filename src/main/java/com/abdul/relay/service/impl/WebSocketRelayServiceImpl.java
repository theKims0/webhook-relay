package com.abdul.relay.service.impl;

import com.abdul.relay.dto.RelayRequestDTO;
import com.abdul.relay.dto.RelayResponse;
import com.abdul.relay.dto.RelayResponseDto;
import com.abdul.relay.handler.RelayAgentHandler;
import com.abdul.relay.service.WebSocketRelayService;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementasi WebSocketRelayService.
 *
 * <p>Proses relay per request:</p>
 * <ol>
 *   <li>Generate messageId unik (UUID)</li>
 *   <li>Bungkus request ke dalam {@link RelayRequestDTO} dan serialize ke JSON</li>
 *   <li>Kirim JSON ke WebSocket session milik agent (berdasarkan projectId)</li>
 *   <li>Daftarkan {@link CompletableFuture} ke {@link RelayAgentHandler} (key: messageId)</li>
 *   <li>Block sampai response datang atau timeout 30 detik</li>
 *   <li>Map {@link RelayResponseDto} ke {@link RelayResponse} dan return</li>
 * </ol>
 */
@Service
public class WebSocketRelayServiceImpl implements WebSocketRelayService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketRelayServiceImpl.class);
    private static final int TIMEOUT_SECONDS = 30;

    private final RelayAgentHandler relayAgentHandler;
    private final ObjectMapper objectMapper;

    public WebSocketRelayServiceImpl(RelayAgentHandler relayAgentHandler) {
        this.relayAgentHandler = relayAgentHandler;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public RelayResponse relay(String projectId, String method, String path,
                               String headers, String body)
            throws TimeoutException, Exception {

        WebSocketSession session = relayAgentHandler.getSession(projectId);
        if (session == null || !session.isOpen()) {
            log.warn("[Relay] Tidak ada agent aktif untuk projectId: {}", projectId);
            throw new IllegalStateException("Tidak ada Relay Agent yang terhubung untuk project ini.");
        }

        // 2. Buat messageId unik per request
        String messageId = UUID.randomUUID().toString();

        // 3. Bangun payload request
        RelayRequestDTO requestDTO = RelayRequestDTO.builder()
                .messageId(messageId)
                .method(method)
                .path(path)
                .headers(headers != null ? headers : "{}")
                .body(body != null ? body : "")
                .timestamp(Instant.now().toString())
                .build();

        String payload;
        try {
            payload = objectMapper.writeValueAsString(requestDTO);
        } catch (Exception e) {
            log.error("[Relay] Gagal serialize request ke JSON: {}", e.getMessage());
            throw new Exception("Gagal membangun relay request.", e);
        }


        CompletableFuture<RelayResponseDto> future = new CompletableFuture<>();
        relayAgentHandler.registerPendingResponse(messageId, future);


        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
            log.debug("[Relay] Request dikirim ke agent — projectId: {}, messageId: {}, method: {}, path: {}",
                    projectId, messageId, method, path);
        } catch (Exception e) {
            relayAgentHandler.removePendingResponse(messageId);
            log.error("[Relay] Gagal mengirim pesan ke agent: {}", e.getMessage());
            throw new Exception("Gagal mengirim request ke Relay Agent.", e);
        }

        // 6. Tunggu response dari agent (blocking dengan timeout)
        RelayResponseDto responseDto;
        try {
            responseDto = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            relayAgentHandler.removePendingResponse(messageId);
            log.warn("[Relay] Timeout menunggu response dari agent — projectId: {}, messageId: {}",
                    projectId, messageId);
            throw new TimeoutException("Agent tidak merespons dalam " + TIMEOUT_SECONDS + " detik.");
        } catch (Exception e) {
            relayAgentHandler.removePendingResponse(messageId);
            log.error("[Relay] Error saat menunggu response: {}", e.getMessage());
            throw new Exception("Error saat menunggu response dari Relay Agent.", e);
        }

        // 7. Map DTO ke RelayResponse
        log.info("[Relay] Response diterima — messageId: {}, statusCode: {}",
                messageId, responseDto.getStatusCode());

        return RelayResponse.builder()
                .statusCode(responseDto.getStatusCode())
                .body(responseDto.getBody())
                .headers(responseDto.getHeaders())
                .build();
    }
}
