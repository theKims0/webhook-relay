package com.abdul.relay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                // Nonaktifkan CSRF untuk endpoint relay (diakses oleh service lain, bukan browser)
                // dan WebSocket handshake
                .ignoringRequestMatchers("/v1/gateway/**", "/ws/agent")
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/register", "/auth/register", "/login",
                    "/css/**", "/js/**", "/images/**", "/webjars/**",
                    "/ws/agent",          // WebSocket endpoint untuk Relay Agent
                    "/v1/gateway/**"      // Relay gateway — autentikasi via X-Relay-Token
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll());

        return http.build();
    }

    /**
     * CORS: izinkan Angular frontend dan service lain mengakses relay gateway.
     * Tambahkan origin baru jika ada frontend/service lain yang perlu akses.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origin yang diizinkan (tambahkan sesuai kebutuhan)
        config.setAllowedOrigins(List.of(
            "http://localhost:4200",   // Angular dev server
            "http://localhost:3000",   // React / Next.js dev server
            "http://localhost:5173"    // Vite dev server
        ));

        // Method yang diizinkan
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Header yang diizinkan dikirim oleh client
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Relay-Token",
            "X-Requested-With"
        ));

        // Izinkan browser kirim cookies / auth headers
        config.setAllowCredentials(true);

        // Cache preflight response selama 1 jam
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Terapkan CORS ke semua endpoint
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}

