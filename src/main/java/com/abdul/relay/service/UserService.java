package com.abdul.relay.service;

import com.abdul.relay.dto.LoginRequestDTO;
import com.abdul.relay.dto.RegisterRequestDTO;
import com.abdul.relay.entity.User;

public interface UserService {
    void register(RegisterRequestDTO registerRequestDTO) throws Exception;
    boolean login(LoginRequestDTO loginRequestDto);
    User findUserByUsername(String username);
    User findUserById(String userId);
}
