package com.extratoPopular.interfaces.controller;

import com.extratoPopular.application.usecase.ChatFinanceService;
import com.extratoPopular.application.usecase.ReportFinanceService;
import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.enums.FonteImportacao;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.exception.AiIntegrationException;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.OrcamentoRepository;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Segurança multi-tenant — /api/v1/chat e /api/v1/relatorio")
class AiControllerMultiTenantIntegrationTest {

    @Autowired private MockMvc             mockMvc;
    @Autowired private UserRepository      userRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private OrcamentoRepository orcamentoRepository;
    @Autowired private JwtService          jwtService;
    @Autowired private PasswordEncoder     passwordEncoder;
    @Autowired private ObjectMapper        objectMapper;

    @MockBean private ChatFinanceService   chatFinanceService;
    @MockBean private ReportFinanceService reportFinanceService;

    private String tokenUsuarioA;
    private String tokenUsuarioB;
    private User   salvoA;
    private User   salvoB;

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();

        User usuarioA = new User();
        usuarioA.setNome("Usuario AI A");
        usuarioA.setEmail("ai-a@email.com");
        usuarioA.setSenha(passwordEncoder.encode("senha123"));
        usuarioA.setRendaMensal(new BigDecimal("5000.00"));
        salvoA = userRepository.save(usuarioA);
        tokenUsuarioA = jwtService.generateToken(salvoA.getId());
        criarTransacao(salvoA.getId(), new BigDecimal("-300.00"), "SUPERMERCADO USUARIO A", Categoria.ALIMENTACAO);

        User usuarioB = new User();
        usuarioB.setNome("Usuario AI B");
        usuarioB.setEmail("ai-b@email.com");
        usuarioB.setSenha(passwordEncoder.encode("senha123"));
        usuarioB.setRendaMensal(new BigDecimal("3000.00"));
        salvoB = userRepository.save(usuarioB);
        tokenUsuarioB = jwtService.generateToken(salvoB.getId());
        criarTransacao(salvoB.getId(), new BigDecimal("-150.00"), "POSTO USUARIO B", Categoria.TRANSPORTE);
    }

    @AfterEach
    void tearDown() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // POST /api/v1/chat — userId capturado
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/chat — userId passado ao service é o do usuário A autenticado")
    void chat_deve_passar_userId_do_usuario_a() throws Exception {
        when(chatFinanceService.chat(anyLong(), anyString())).thenReturn("ok");

        mockMvc.perform(post("/api/v1/chat")
                        .header("Authorization", "Bearer " + tokenUsuarioA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "teste"))))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(chatFinanceService).chat(userIdCaptor.capture(), anyString());

        assertThat(userIdCaptor.getValue()).isEqualTo(salvoA.getId());
    }

    @Test
    @DisplayName("POST /api/v1/chat — userId passado ao service é o do usuário B autenticado")
    void chat_deve_passar_userId_do_usuario_b() throws Exception {
        when(chatFinanceService.chat(anyLong(), anyString())).thenReturn("ok");

        mockMvc.perform(post("/api/v1/chat")
                        .header("Authorization", "Bearer " + tokenUsuarioB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "teste"))))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(chatFinanceService).chat(userIdCaptor.capture(), anyString());

        assertThat(userIdCaptor.getValue()).isEqualTo(salvoB.getId());
    }

    // =========================================================================
    // GET /api/v1/relatorio — userId capturado
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/relatorio — userId passado ao service é o do usuário A autenticado")
    void relatorio_deve_passar_userId_do_usuario_a() throws Exception {
        when(reportFinanceService.generateReport(anyLong())).thenReturn("ok");

        mockMvc.perform(get("/api/v1/relatorio")
                        .header("Authorization", "Bearer " + tokenUsuarioA))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(reportFinanceService).generateReport(userIdCaptor.capture());

        assertThat(userIdCaptor.getValue()).isEqualTo(salvoA.getId());
    }

    @Test
    @DisplayName("GET /api/v1/relatorio — userId passado ao service é o do usuário B autenticado")
    void relatorio_deve_passar_userId_do_usuario_b() throws Exception {
        when(reportFinanceService.generateReport(anyLong())).thenReturn("ok");

        mockMvc.perform(get("/api/v1/relatorio")
                        .header("Authorization", "Bearer " + tokenUsuarioB))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(reportFinanceService).generateReport(userIdCaptor.capture());

        assertThat(userIdCaptor.getValue()).isEqualTo(salvoB.getId());
    }

    // =========================================================================
    // Autenticação obrigatória
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/chat — deve retornar 401 sem token")
    void chat_sem_token_deve_retornar_401() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "teste"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/relatorio — deve retornar 401 sem token")
    void relatorio_sem_token_deve_retornar_401() throws Exception {
        mockMvc.perform(get("/api/v1/relatorio"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Tratamento de falha da IA
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/chat — deve retornar 503 quando ChatFinanceService lançar AiIntegrationException")
    void chat_deve_retornar_503_quando_service_falhar() throws Exception {
        when(chatFinanceService.chat(anyLong(), anyString()))
                .thenThrow(new AiIntegrationException("Falha", null));

        mockMvc.perform(post("/api/v1/chat")
                        .header("Authorization", "Bearer " + tokenUsuarioA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "teste"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("GET /api/v1/relatorio — deve retornar 503 quando ReportFinanceService lançar AiIntegrationException")
    void relatorio_deve_retornar_503_quando_service_falhar() throws Exception {
        when(reportFinanceService.generateReport(anyLong()))
                .thenThrow(new AiIntegrationException("Falha", null));

        mockMvc.perform(get("/api/v1/relatorio")
                        .header("Authorization", "Bearer " + tokenUsuarioA))
                .andExpect(status().isServiceUnavailable());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void criarTransacao(Long userId, BigDecimal valor, String descricao, Categoria categoria) {
        Transacao t = new Transacao();
        t.setUserId(userId);
        t.setData(LocalDate.now());
        t.setValor(valor);
        t.setDescricao(descricao);
        t.setCategoria(categoria);
        t.setTipo(valor.compareTo(BigDecimal.ZERO) < 0 ? TipoTransacao.DEBITO : TipoTransacao.CREDITO);
        t.setFonte(FonteImportacao.MANUAL);
        t.setHash(String.valueOf((userId + descricao).hashCode()));
        transacaoRepository.save(t);
    }
}
