package com.extratoPopular.interfaces.controller;

import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Setup: Limpamos o banco e criamos um estado conhecido antes de cada teste
        userRepository.deleteAll();
        User user = new User();
        user.setNome("Usuário Existente");
        user.setEmail("existente@email.com");
        user.setSenha(passwordEncoder.encode("senha123"));
        user.setRendaMensal(new BigDecimal("3000.00"));
        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        // Teardown: Garantimos que nenhuma sujeira passe para o próximo teste
        userRepository.deleteAll();
    }

    @Test
    void deve_retornar201_quando_registrarComDadosValidos() throws Exception {
        Map<String, Object> request = Map.of(
                "nome", "Novo Usuário",
                "email", "novo@email.com",
                "senha", "senhaForte123!",
                "rendaMensal", 2500.00
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("novo@email.com"));
    }

    @Test
    void deve_retornar409_quando_registrarComEmailDuplicado() throws Exception {
        Map<String, Object> request = Map.of(
                "nome", "Outro Nome",
                "email", "existente@email.com",
                "senha", "senhaForte123!",
                "rendaMensal", 2500.00
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("EMAIL_JA_CADASTRADO"));
    }

    @Test
    void deve_retornar400_quando_registrarComEmailMalformado() throws Exception {
        Map<String, Object> request = Map.of(
                "nome", "Usuário Teste",
                "email", "email-invalido",
                "senha", "senha123",
                "rendaMensal", 2500.00
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_FALHOU"))
                .andExpect(jsonPath("$.detalhes").exists());
    }

    @Test
    void deve_retornar200_quando_loginComSenhaCorreta() throws Exception {
        Map<String, String> request = Map.of(
                "email", "existente@email.com",
                "senha", "senha123"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("existente@email.com"));
    }

    @Test
    void deve_retornar401_quando_loginComSenhaIncorreta() throws Exception {
        Map<String, String> request = Map.of(
                "email", "existente@email.com",
                "senha", "senhaErrada"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    void deve_retornar401_quando_acessarPerfilSemToken() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deve_retornar200_quando_acessarPerfilComTokenValido() throws Exception {
        User user = userRepository.findByEmail("existente@email.com").orElseThrow();
        String token = jwtService.generateToken(user.getId());

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.nome").value("Usuário Existente"))
                .andExpect(jsonPath("$.email").value("existente@email.com"));
    }

    @Test
    void deve_retornar401_quando_acessarPerfilComTokenInvalido() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer token.totalmente.invalido")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
