package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.exception.CredenciaisInvalidasException;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.infrastructure.security.JwtService;
import com.extratoPopular.interfaces.dto.AuthResponse;
import com.extratoPopular.interfaces.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private LoginUserUseCase loginUserUseCase;

    private LoginRequest request;
    private User user;

    @BeforeEach
    void setUp() {
        request = new LoginRequest("maria@email.com", "senha123");
        
        user = new User();
        user.setId(1L);
        user.setEmail("maria@email.com");
        user.setSenha("senhaEncriptada");
    }

    @Test
    void deve_retornarJwt_quando_credenciaisCorretas() {
        // Arrange
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.senha(), user.getSenha())).thenReturn(true);
        when(jwtService.generateToken(user.getId())).thenReturn("tokenJWT");

        // Act
        AuthResponse response = loginUserUseCase.execute(request);

        // Assert
        assertNotNull(response);
        assertEquals("tokenJWT", response.token());
        assertEquals(1L, response.userId());
        assertEquals("maria@email.com", response.email());
        
        verify(userRepository, times(1)).findByEmail(request.email());
        verify(passwordEncoder, times(1)).matches(request.senha(), user.getSenha());
        verify(jwtService, times(1)).generateToken(user.getId());
    }

    @Test
    void deve_lancarCredenciaisInvalidasException_quando_senhaIncorreta() {
        // Arrange
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.senha(), user.getSenha())).thenReturn(false);

        // Act & Assert
        assertThrows(CredenciaisInvalidasException.class, () -> loginUserUseCase.execute(request));
        
        verify(userRepository, times(1)).findByEmail(request.email());
        verify(passwordEncoder, times(1)).matches(request.senha(), user.getSenha());
        verify(jwtService, never()).generateToken(anyLong());
    }

    @Test
    void deve_lancarCredenciaisInvalidasException_quando_emailNaoEncontrado() {
        // Arrange
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CredenciaisInvalidasException.class, () -> loginUserUseCase.execute(request));
        
        verify(userRepository, times(1)).findByEmail(request.email());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyLong());
    }
}
