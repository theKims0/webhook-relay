package com.abdul.relay.service.impl;

import com.abdul.relay.dto.RegisterRequestDTO;
import com.abdul.relay.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setEmail("sama@example.com");
        dto.setPassword("password123");
        dto.setUsername("relayuser");
        dto.setConfirmPassword("password123");

        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(dto));
        verify(userRepository).existsByEmailIgnoreCase("sama@example.com");
        verifyNoInteractions(passwordEncoder);
    }
}
