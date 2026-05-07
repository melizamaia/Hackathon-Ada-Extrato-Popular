package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.exception.CredenciaisInvalidasException;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.infrastructure.security.JwtService;
import com.extratoPopular.interfaces.dto.AuthResponse;
import com.extratoPopular.interfaces.dto.LoginRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse execute(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(request.senha(), user.getSenha())) {
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos.");
        }

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token, user.getId(), user.getEmail());
    }
}
