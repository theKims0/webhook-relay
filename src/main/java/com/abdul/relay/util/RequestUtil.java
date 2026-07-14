package com.abdul.relay.util;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestUtil {
    public static String extractBody(HttpServletRequest request) {
        try {
            return request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            return "";
        }
    }

    public static String extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name ->
                headers.put(name, request.getHeader(name))
        );
        // Buang header internal yang tidak perlu diteruskan
        headers.remove("x-relay-token");
        headers.remove("host");
        try {
            return new ObjectMapper().writeValueAsString(headers);
        } catch (Exception e) {
            return "{}";
        }
    }
}
