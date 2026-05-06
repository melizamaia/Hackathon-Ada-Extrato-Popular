package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.exception.EmailJaCadastradoException;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.infrastructure.security.JwtService;
import com.extratoPopular.interfaces.dto.AuthResponse;
import com.extratoPopular.interfaces.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse execute(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailJaCadastradoException("O e-mail informado já está em uso.");
        }

        User user = new User();
        user.setNome(request.nome());
        user.setEmail(request.email());
        user.setSenha(passwordEncoder.encode(request.senha()));
        user.setRendaMensal(request.rendaMensal());

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId());
        return new AuthResponse(token);
    }
}
