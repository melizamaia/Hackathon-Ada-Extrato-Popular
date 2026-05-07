package com.extratoPopular.interfaces.controller;

import com.extratoPopular.application.usecase.LoginUserUseCase;
import com.extratoPopular.application.usecase.RegisterUserUseCase;
import com.extratoPopular.interfaces.dto.AuthResponse;
import com.extratoPopular.interfaces.dto.LoginRequest;
import com.extratoPopular.interfaces.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.extratoPopular.application.usecase.GetUserProfileUseCase;
import com.extratoPopular.interfaces.dto.UserProfileResponse;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints para registro e login de usuários")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase, LoginUserUseCase loginUserUseCase, GetUserProfileUseCase getUserProfileUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.getUserProfileUseCase = getUserProfileUseCase;
    }

    @Operation(summary = "Registrar novo usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação dos dados enviados", content = @Content),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = registerUserUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login de usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação dos dados enviados", content = @Content),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = loginUserUseCase.execute(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obter perfil do usuário logado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Perfil recuperado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado (Token ausente ou inválido)", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {
        UserProfileResponse response = getUserProfileUseCase.execute();
        return ResponseEntity.ok(response);
    }
}
