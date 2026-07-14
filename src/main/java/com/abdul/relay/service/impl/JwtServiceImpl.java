package com.abdul.relay.service.impl;

import com.abdul.relay.entity.User;
import com.abdul.relay.service.JwtService;
import com.abdul.relay.service.RedisService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${relay.jwt-secret}")
    private String SECRET_KEY;
    @Value("${relay.jwt-expiration-in-day-access-token}")
    private Integer TIME_EXPIRED_DAY;
    @Value("${relay.jwt-issuer}")
    private String ISSUER;



    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            return JWT.create()
                    .withSubject(user.getId().toString())
                    .withIssuer(ISSUER)
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(Instant.now().plus(TIME_EXPIRED_DAY, ChronoUnit.DAYS))
                    .withClaim("role", "ROLE_USER")
                    .sign(algorithm);
        }catch (JWTCreationException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "FAILED GENERATE JWT");
        }
    }
    @Override
    public String getUserId(String token) {
        DecodedJWT decodedJWT = claimJwt(token);
        if (decodedJWT != null) return decodedJWT.getSubject();
        return null;
    }
    public DecodedJWT claimJwt(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
            return verifier.verify(token);
        }catch (Exception e){
            return  null;
        }
    }
}
