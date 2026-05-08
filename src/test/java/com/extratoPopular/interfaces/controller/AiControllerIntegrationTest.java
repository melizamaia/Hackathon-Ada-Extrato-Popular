package com.extratoPopular.interfaces.controller;

import com.extratoPopular.application.usecase.ChatFinanceService;
import com.extratoPopular.application.usecase.ReportFinanceService;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.ai.OpenAiClient;
import com.extratoPopular.infrastructure.persistence.OrcamentoRepository;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiControllerIntegrationTest {

    @Autowired private MockMvc         mockMvc;
    @Autowired private UserRepository  userRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private OrcamentoRepository orcamentoRepository;
    @Autowired private JwtService      jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper    objectMapper;

    @MockBean private ChatFinanceService   chatFinanceService;
    @MockBean private ReportFinanceService reportFinanceService;
    @MockBean private OpenAiClient         openAiClient;

    private static final String CHAT_RESPONSE    = "Resposta do assistente financeiro.";
    private static final String REPORT_RESPONSE  = "Relatório financeiro personalizado.";

    private String token;

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setNome("Usuário AI Test");
        user.setEmail("ai-test@email.com");
        user.setSenha(passwordEncoder.encode("senha123"));
        user.setRendaMensal(new BigDecimal("3000.00"));
        User salvo = userRepository.save(user);
        token = jwtService.generateToken(salvo.getId());

        when(chatFinanceService.chat(anyLong(), anyString()))
                .thenReturn(CHAT_RESPONSE);
        when(reportFinanceService.generateReport(anyLong()))
                .thenReturn(REPORT_RESPONSE);
    }

    @AfterEach
    void tearDown() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // POST /api/v1/chat
    // =========================================================================

    @Test
    void deve_retornar_200_com_resposta_do_chat_autenticado() throws Exception {
        Map<String, String> body = Map.of("message", "Qual meu maior gasto?");

        mockMvc.perform(post("/api/v1/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value(CHAT_RESPONSE))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deve_retornar_400_quando_mensagem_vazia() throws Exception {
        Map<String, String> body = Map.of("message", "");

        mockMvc.perform(post("/api/v1/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deve_retornar_401_ao_enviar_chat_sem_token() throws Exception {
        Map<String, String> body = Map.of("message", "Qual meu maior gasto?");

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // GET /api/v1/relatorio
    // =========================================================================

    @Test
    void deve_retornar_200_com_relatorio_autenticado() throws Exception {
        mockMvc.perform(get("/api/v1/relatorio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorio").value(REPORT_RESPONSE));
    }

    @Test
    void deve_retornar_401_ao_acessar_relatorio_sem_token() throws Exception {
        mockMvc.perform(get("/api/v1/relatorio"))
                .andExpect(status().isUnauthorized());
    }
}
