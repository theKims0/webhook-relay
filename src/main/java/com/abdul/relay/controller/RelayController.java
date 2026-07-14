package com.abdul.relay.controller;

import com.abdul.relay.dto.RelayResponse;
import com.abdul.relay.entity.Project;
import com.abdul.relay.entity.User;
import com.abdul.relay.service.JwtService;
import com.abdul.relay.service.ProjectService;
import com.abdul.relay.service.UserService;
import com.abdul.relay.service.WebSocketRelayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeoutException;

import static com.abdul.relay.util.RequestUtil.extractBody;
import static com.abdul.relay.util.RequestUtil.extractHeaders;

/**
 * Gateway controller — menerima semua request HTTP masuk dan meneruskannya
 * ke Relay Agent yang terhubung via WebSocket.
 *
 * <p>Format URL: {@code /v1/gateway/{slug}/**}</p>
 * <p>Contoh: {@code POST /v1/gateway/my-project/api/users} akan meneruskan
 * {@code POST /api/users} ke service lokal yang terhubung melalui agent.</p>
 */
@RestController
@RequestMapping("/v1/gateway")
@RequiredArgsConstructor
public class RelayController {

    private final ProjectService projectService;
    private final JwtService jwtService;
    private final UserService userService;
    private final WebSocketRelayService webSocketRelayService;

    @RequestMapping(
            value   = { "/{slug}", "/{slug}/**" },
            method  = { RequestMethod.POST, RequestMethod.GET,
                        RequestMethod.PUT,  RequestMethod.DELETE,
                        RequestMethod.PATCH, RequestMethod.OPTIONS }
    )
    public ResponseEntity<String> relay(
            @PathVariable("slug")                         String slug,
            @RequestHeader(value = "X-Relay-Token",
                    required = false)                     String relayToken,
            HttpServletRequest request) {


        // 1. Validasi token
        if (relayToken == null || relayToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"MISSING_TOKEN\",\"message\":\"Header X-Relay-Token wajib disertakan.\"}");
        }

        String userId = jwtService.getUserId(relayToken);
        Boolean isTokenValid = projectService.isTokenValid(relayToken, slug, userId);
        if (!isTokenValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"INVALID_TOKEN\",\"message\":\"Relay token tidak sesuai.\"}");
        }

        // 2. Cari project
        User user = userService.findUserById(userId);
        Project project;
        try {
            project = projectService.getProjectBySlugAndUser(slug, user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"PROJECT_NOT_FOUND\",\"message\":\"Project tidak ditemukan.\"}");
        }
        System.out.println(project.getIsActive());
        if (!project.getIsActive()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"PROJECT_DISABLED\",\"message\":\"Project sedang nonaktif.\"}");
        }

        // 3. Cek koneksi agent
        try {
            String body    = extractBody(request);
            String headers = extractHeaders(request);
            String method  = request.getMethod();

            String forwardPath = extractForwardPath(request.getRequestURI(), slug);
            RelayResponse response = webSocketRelayService.relay(
                    project.getId().toString(), method, forwardPath, headers, body);

            return ResponseEntity
                    .status(response.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());

        } catch (IllegalStateException e) {

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"AGENT_NOT_CONNECTED\",\"message\":\"" + e.getMessage() + "\"}");

        } catch (TimeoutException e) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"GATEWAY_TIMEOUT\",\"message\":\"Agent tidak merespons dalam 30 detik.\"}");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"INTERNAL_ERROR\",\"message\":\"Terjadi kesalahan internal.\"}");
        }
    }

    private String extractForwardPath(String requestUri, String slug) {
        String prefix = "/v1/gateway/" + slug;
        if (requestUri.startsWith(prefix)) {
            String remaining = requestUri.substring(prefix.length());
            return remaining.isEmpty() ? "/" : remaining;
        }
        return requestUri;
    }
}
