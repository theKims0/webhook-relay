package com.abdul.relay.service.impl;

import com.abdul.relay.dto.LoginRequestDTO;
import com.abdul.relay.dto.RegisterRequestDTO;
import com.abdul.relay.entity.User;
import com.abdul.relay.repository.UserRepository;
import com.abdul.relay.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    public UserServiceImpl(PasswordEncoder passwordEncoder, UserRepository userRepository, AuthenticationManager authenticationManager) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public void register(RegisterRequestDTO registerRequestDTO) throws Exception {
        System.out.println("Masuk Register");
        if (userRepository.existsByEmailIgnoreCase(registerRequestDTO.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar");
        }
        System.out.println("register ini nih");
        if (userRepository.existsByUsernameIgnoreCase(registerRequestDTO.getUsername())) {
            throw new IllegalArgumentException("Username sudah terdaftar");
        }
        try {System.out.println("register ini nih");
            User user = mapRegisterDtoToUser(registerRequestDTO);
            userRepository.save(user);
        }catch (Exception e){
            throw new Exception(e.getMessage());
        }
        System.out.println("register ini nih");
    }



    @Override
    public boolean login(LoginRequestDTO loginRequestDto) {
        if (loginRequestDto == null || loginRequestDto.getUsername() == null || loginRequestDto.getPassword() == null) {
            return false;
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequestDto.getUsername(), loginRequestDto.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            return true;
        }catch (Exception e){
            e.getMessage();
            return false;
        }

    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsernameAndIsActive(username, true);
    }

    @Override
    public User findUserById(String userId) {
        UUID userIdUUID = UUID.fromString(userId);
        return userRepository.findUserById(userIdUUID);
    }

    public User mapRegisterDtoToUser(RegisterRequestDTO registerRequestDTO){
        String passwordHash = passwordEncoder.encode(registerRequestDTO.getPassword());
        User user = new User();
        user.setUsername(registerRequestDTO.getUsername());
        user.setEmail(registerRequestDTO.getEmail());
        user.setIsActive(true);
        user.setPassword(passwordHash);
        LocalDateTime waktuSekarang = LocalDateTime.now();
        user.setCreatedAt(Timestamp.valueOf(waktuSekarang));
        return user;
    }
}
