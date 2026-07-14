package com.abdul.relay.handler;

import com.abdul.relay.dto.RelayResponseDto;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler WebSocket untuk koneksi dari Relay Agent.
 *
 * <p>Alur kerja:</p>
 * <ol>
 *   <li>Agent connect ke {@code /ws/agent?projectId={uuid}}</li>
 *   <li>Session disimpan di {@code agentSessions} berdasarkan projectId</li>
 *   <li>Saat ada relay request, service mengirim pesan JSON ke session agent</li>
 *   <li>Agent merespons dengan JSON {@link RelayResponseDto}</li>
 *   <li>Response di-dispatch ke {@code CompletableFuture} yang menunggu (berdasarkan messageId)</li>
 * </ol>
 */
@Component
public class RelayAgentHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RelayAgentHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Menyimpan sesi WebSocket agent yang aktif.
     * Key: projectId (UUID string), Value: WebSocketSession
     */
    private final Map<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();

    private final Map<String, CompletableFuture<RelayResponseDto>> pendingResponses = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String projectId = extractProjectId(session);
        if (projectId == null || projectId.isBlank()) {
            log.warn("[WS] Agent connect tanpa projectId — session ditolak: {}", session.getId());
            closeQuietly(session);
            return;
        }

        // Jika ada sesi lama untuk projectId yang sama, tutup dulu
        WebSocketSession existing = agentSessions.get(projectId);
        if (existing != null && existing.isOpen()) {
            log.info("[WS] Sesi lama untuk projectId {} ditemukan, mengganti dengan sesi baru.", projectId);
            closeQuietly(existing);
        }

        agentSessions.put(projectId, session);
        log.info("[WS] Agent terhubung — projectId: {}, sessionId: {}", projectId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String projectId = extractProjectId(session);
        if (projectId != null) {
            agentSessions.remove(projectId);
            log.info("[WS] Agent terputus — projectId: {}, status: {}", projectId, status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            RelayResponseDto responseDto = objectMapper.readValue(message.getPayload(), RelayResponseDto.class);
            String messageId = responseDto.getMessageId();

            if (messageId == null || messageId.isBlank()) {
                log.warn("[WS] Response dari agent tidak memiliki messageId: {}", message.getPayload());
                return;
            }

            CompletableFuture<RelayResponseDto> future = pendingResponses.remove(messageId);
            if (future != null) {
                future.complete(responseDto);
                log.debug("[WS] Response untuk messageId {} berhasil diteruskan.", messageId);
            } else {
                log.warn("[WS] Tidak ada pending request untuk messageId: {}", messageId);
            }

        } catch (Exception e) {
            log.error("[WS] Gagal mem-parse response dari agent: {}", e.getMessage(), e);
        }
    }


    public WebSocketSession getSession(String projectId) {
        WebSocketSession session = agentSessions.get(projectId);
        if (session != null && session.isOpen()) {
            return session;
        }
        agentSessions.remove(projectId); // bersihkan sesi yang sudah tidak valid
        return null;
    }

    public void registerPendingResponse(String messageId, CompletableFuture<RelayResponseDto> future) {
        pendingResponses.put(messageId, future);
    }

    public void removePendingResponse(String messageId) {
        pendingResponses.remove(messageId);
    }

    private String extractProjectId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery(); // "projectId=xxx"
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "projectId".equalsIgnoreCase(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (Exception ignored) {
        }
    }
}
