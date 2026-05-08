package com.extratoPopular.interfaces.controller;

import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.enums.FonteImportacao;
import com.extratoPopular.domain.enums.TipoTransacao;
import com.extratoPopular.domain.model.Transacao;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.ai.OpenAiClient;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa isolamento multi-tenant: garante que o contexto financeiro enviado à IA
 * contém apenas dados do usuário autenticado, nunca de outro usuário.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Segurança multi-tenant — chat e relatório")
class ChatMultiTenantIntegrationTest {

    @Autowired private MockMvc             mockMvc;
    @Autowired private UserRepository      userRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private OrcamentoRepository orcamentoRepository;
    @Autowired private JwtService          jwtService;
    @Autowired private PasswordEncoder     passwordEncoder;
    @Autowired private ObjectMapper        objectMapper;

    @MockBean
    private OpenAiClient openAiClient;

    private String tokenUsuarioA;
    private String tokenUsuarioB;

    // Valores únicos e distintos por usuário — facilitam assertivas
    private static final BigDecimal RENDA_A       = new BigDecimal("7777.00");
    private static final BigDecimal RENDA_B       = new BigDecimal("2222.00");
    private static final BigDecimal GASTO_A       = new BigDecimal("-500.00");
    private static final BigDecimal GASTO_B       = new BigDecimal("-100.00");

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();

        // Usuário A
        User usuarioA = new User();
        usuarioA.setNome("Usuario A");
        usuarioA.setEmail("a@email.com");
        usuarioA.setSenha(passwordEncoder.encode("senha123"));
        usuarioA.setRendaMensal(RENDA_A);
        User salvoA = userRepository.save(usuarioA);
        tokenUsuarioA = jwtService.generateToken(salvoA.getId());
        criarTransacao(salvoA.getId(), GASTO_A, "SUPERMERCADO USUARIO A", Categoria.ALIMENTACAO);

        // Usuário B
        User usuarioB = new User();
        usuarioB.setNome("Usuario B");
        usuarioB.setEmail("b@email.com");
        usuarioB.setSenha(passwordEncoder.encode("senha123"));
        usuarioB.setRendaMensal(RENDA_B);
        User salvoB = userRepository.save(usuarioB);
        tokenUsuarioB = jwtService.generateToken(salvoB.getId());
        criarTransacao(salvoB.getId(), GASTO_B, "POSTO USUARIO B", Categoria.TRANSPORTE);

        when(openAiClient.gerarResposta(anyString())).thenReturn("resposta mockada");
    }

    @AfterEach
    void tearDown() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // /chat — isolamento de contexto
    // =========================================================================

    @Test
    @DisplayName("POST /chat — contexto enviado à IA contém apenas dados do usuário A (ALIMENTACAO)")
    void chat_deve_enviar_apenas_dados_do_usuario_autenticado() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(openAiClient.gerarResposta(captor.capture())).thenReturn("ok");

        mockMvc.perform(post("/chat")
                        .header("Authorization", "Bearer " + tokenUsuarioA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pergunta", "teste"))))
                .andExpect(status().isOk());

        String promptEnviado = captor.getValue();

        assertThat(promptEnviado)
                .as("Prompt deve conter a categoria do usuário A")
                .contains("ALIMENTACAO");

        assertThat(promptEnviado)
                .as("Prompt NÃO deve conter a categoria exclusiva do usuário B")
                .doesNotContain("TRANSPORTE");
    }

    @Test
    @DisplayName("POST /chat — contexto enviado à IA contém apenas dados do usuário B (TRANSPORTE)")
    void chat_deve_enviar_apenas_dados_do_usuario_b_quando_autenticado_como_b() throws Exception {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(openAiClient.gerarResposta(captor.capture())).thenReturn("ok");

        mockMvc.perform(post("/chat")
                        .header("Authorization", "Bearer " + tokenUsuarioB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pergunta", "teste"))))
                .andExpect(status().isOk());

        String promptEnviado = captor.getValue();

        assertThat(promptEnviado)
                .as("Prompt deve conter a categoria do usuário B")
                .contains("TRANSPORTE");

        assertThat(promptEnviado)
                .as("Prompt NÃO deve conter a categoria exclusiva do usuário A")
                .doesNotContain("ALIMENTACAO");
    }

    // =========================================================================
    // /relatorio — isolamento de contexto
    // =========================================================================

    @Test
    @DisplayName("GET /relatorio — contexto enviado à IA não vaza dados do outro usuário")
    void relatorio_nao_deve_vazar_dados_entre_usuarios() throws Exception {
        ArgumentCaptor<String> captorA = ArgumentCaptor.forClass(String.class);
        when(openAiClient.gerarResposta(captorA.capture())).thenReturn("ok");

        mockMvc.perform(get("/relatorio")
                        .header("Authorization", "Bearer " + tokenUsuarioA))
                .andExpect(status().isOk());

        String promptA = captorA.getValue();
        assertThat(promptA).as("Relatório do usuário A não deve conter dados do B").doesNotContain("TRANSPORTE");

        ArgumentCaptor<String> captorB = ArgumentCaptor.forClass(String.class);
        when(openAiClient.gerarResposta(captorB.capture())).thenReturn("ok");

        mockMvc.perform(get("/relatorio")
                        .header("Authorization", "Bearer " + tokenUsuarioB))
                .andExpect(status().isOk());

        String promptB = captorB.getValue();
        assertThat(promptB).as("Relatório do usuário B não deve conter dados do A").doesNotContain("ALIMENTACAO");
    }

    // =========================================================================
    // Autenticação obrigatória
    // =========================================================================

    @Test
    @DisplayName("POST /chat — deve retornar 401 sem token")
    void chat_sem_token_deve_retornar_401() throws Exception {
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pergunta", "teste"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /relatorio — deve retornar 401 sem token")
    void relatorio_sem_token_deve_retornar_401() throws Exception {
        mockMvc.perform(get("/relatorio"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Tratamento de falha da IA
    // =========================================================================

    @Test
    @DisplayName("POST /chat — deve retornar 503 quando IA lançar AiIntegrationException")
    void chat_deve_retornar_503_quando_ia_falhar() throws Exception {
        when(openAiClient.gerarResposta(anyString()))
                .thenThrow(new com.extratoPopular.domain.exception.AiIntegrationException("Timeout", null));

        mockMvc.perform(post("/chat")
                        .header("Authorization", "Bearer " + tokenUsuarioA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("pergunta", "teste"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("GET /relatorio — deve retornar 503 quando IA lançar AiIntegrationException")
    void relatorio_deve_retornar_503_quando_ia_falhar() throws Exception {
        when(openAiClient.gerarResposta(anyString()))
                .thenThrow(new com.extratoPopular.domain.exception.AiIntegrationException("API key inválida", null));

        mockMvc.perform(get("/relatorio")
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
