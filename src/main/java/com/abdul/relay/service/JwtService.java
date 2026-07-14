package com.abdul.relay.service;

import com.abdul.relay.entity.User;

public interface JwtService {
    String generateToken(User user);
    String getUserId(String token);
}
