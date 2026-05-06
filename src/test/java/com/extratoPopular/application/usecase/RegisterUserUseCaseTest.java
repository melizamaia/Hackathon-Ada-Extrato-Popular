package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.exception.EmailJaCadastradoException;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.infrastructure.security.JwtService;
import com.extratoPopular.interfaces.dto.AuthResponse;
import com.extratoPopular.interfaces.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RegisterUserUseCase registerUserUseCase;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest("Maria Silva", "maria@email.com", "senha123", new BigDecimal("2500.00"));
    }

    @Test
    void deve_salvarUsuario_quando_dadosValidos() {
        // Arrange
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.senha())).thenReturn("senhaEncriptada");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setNome(request.nome());
        savedUser.setEmail(request.email());
        savedUser.setSenha("senhaEncriptada");
        savedUser.setRendaMensal(request.rendaMensal());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(1L)).thenReturn("tokenJWT");

        // Act
        AuthResponse response = registerUserUseCase.execute(request);

        // Assert
        assertNotNull(response);
        assertEquals("tokenJWT", response.token());
        assertEquals(1L, response.userId());
        assertEquals("maria@email.com", response.email());
        
        verify(userRepository, times(1)).findByEmail(request.email());
        verify(passwordEncoder, times(1)).encode(request.senha());
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtService, times(1)).generateToken(1L);
    }

    @Test
    void deve_lancarEmailJaCadastradoException_quando_emailExistente() {
        // Arrange
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThrows(EmailJaCadastradoException.class, () -> registerUserUseCase.execute(request));
        
        verify(userRepository, times(1)).findByEmail(request.email());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtService, never()).generateToken(anyLong());
    }
}
