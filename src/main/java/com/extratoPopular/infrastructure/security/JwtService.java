package com.extratoPopular.infrastructure.security;

public interface JwtService {
    String generateToken(Long userId);
}
