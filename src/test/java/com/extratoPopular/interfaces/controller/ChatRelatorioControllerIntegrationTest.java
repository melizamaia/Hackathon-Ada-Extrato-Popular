package com.extratoPopular.interfaces.controller;

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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatRelatorioControllerIntegrationTest {

    @Autowired private MockMvc             mockMvc;
    @Autowired private UserRepository      userRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private OrcamentoRepository orcamentoRepository;
    @Autowired private JwtService          jwtService;
    @Autowired private PasswordEncoder     passwordEncoder;
    @Autowired private ObjectMapper        objectMapper;

    @MockBean
    private OpenAiClient openAiClient;

    private static final String RESPOSTA_IA = "Resposta mockada da IA para testes.";

    private String token;

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setNome("Usuário IA");
        user.setEmail("ia@email.com");
        user.setSenha(passwordEncoder.encode("senha123"));
        user.setRendaMensal(new BigDecimal("3000.00"));
        User salvo = userRepository.save(user);
        token = jwtService.generateToken(salvo.getId());

        when(openAiClient.gerarResposta(anyString())).thenReturn(RESPOSTA_IA);
    }

    @AfterEach
    void tearDown() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // POST /chat
    // =========================================================================

    @Test
    void deve_retornar_401_ao_enviar_chat_sem_token() throws Exception {
        Map<String, String> body = Map.of("pergunta", "Qual meu maior gasto?");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deve_retornar_200_com_resposta_da_ia_no_chat() throws Exception {
        Map<String, String> body = Map.of("pergunta", "Qual meu maior gasto?");

        mockMvc.perform(post("/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value(RESPOSTA_IA));
    }

    @Test
    void deve_retornar_resposta_nao_nula_no_chat() throws Exception {
        Map<String, String> body = Map.of("pergunta", "Como posso economizar?");

        mockMvc.perform(post("/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").exists())
                .andExpect(jsonPath("$.resposta").isNotEmpty());
    }

    @Test
    void deve_retornar_400_quando_chat_receber_user_id_no_body() throws Exception {
        Map<String, Object> body = Map.of(
                "pergunta", "Qual meu maior gasto?",
                "userId", 999L
        );

        mockMvc.perform(post("/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_FALHOU"));
    }

    // =========================================================================
    // GET /relatorio
    // =========================================================================

    @Test
    void deve_retornar_401_ao_acessar_relatorio_sem_token() throws Exception {
        mockMvc.perform(get("/relatorio"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deve_retornar_200_com_relatorio_gerado_pela_ia() throws Exception {
        mockMvc.perform(get("/relatorio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorio").value(RESPOSTA_IA));
    }

    @Test
    void deve_retornar_relatorio_nao_nulo() throws Exception {
        mockMvc.perform(get("/relatorio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatorio").exists())
                .andExpect(jsonPath("$.relatorio").isNotEmpty());
    }
}
