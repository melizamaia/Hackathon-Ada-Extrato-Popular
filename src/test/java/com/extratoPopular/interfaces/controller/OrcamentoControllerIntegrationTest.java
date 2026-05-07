package com.extratoPopular.interfaces.controller;

import com.extratoPopular.domain.model.Orcamento;
import com.extratoPopular.domain.model.User;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrcamentoControllerIntegrationTest {

    @Autowired private MockMvc             mockMvc;
    @Autowired private UserRepository      userRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private OrcamentoRepository orcamentoRepository;
    @Autowired private JwtService          jwtService;
    @Autowired private PasswordEncoder     passwordEncoder;
    @Autowired private ObjectMapper        objectMapper;

    private String token;
    private Long   userId;

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setNome("Usuário Teste");
        user.setEmail("teste@email.com");
        user.setSenha(passwordEncoder.encode("senha123"));
        user.setRendaMensal(new BigDecimal("3000.00"));
        User salvo = userRepository.save(user);
        userId = salvo.getId();
        token  = jwtService.generateToken(salvo.getId());
    }

    @AfterEach
    void tearDown() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Map<String, Object> requestBody(String categoria, double limite, int mes, int ano) {
        return Map.of("categoria", categoria, "valorLimite", limite, "mes", mes, "ano", ano);
    }

    // =========================================================================
    // POST /orcamentos
    // =========================================================================

    @Test
    void deve_retornar_401_ao_criar_sem_token() throws Exception {
        mockMvc.perform(post("/orcamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody("ALIMENTACAO", 500, 5, 2026))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deve_retornar_201_ao_criar_orcamento_valido() throws Exception {
        mockMvc.perform(post("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody("ALIMENTACAO", 500, 5, 2026))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.categoria").value("ALIMENTACAO"))
                .andExpect(jsonPath("$.valorLimite").value(500.0))
                .andExpect(jsonPath("$.mes").value(5))
                .andExpect(jsonPath("$.ano").value(2026));
    }

    @Test
    void deve_retornar_409_ao_criar_orcamento_duplicado() throws Exception {
        String body = objectMapper.writeValueAsString(requestBody("TRANSPORTE", 300, 5, 2026));

        mockMvc.perform(post("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void deve_retornar_400_ao_criar_orcamento_com_mes_invalido() throws Exception {
        mockMvc.perform(post("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody("LAZER", 200, 13, 2026))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("VALIDACAO_FALHOU"));
    }

    // =========================================================================
    // GET /orcamentos
    // =========================================================================

    @Test
    void deve_retornar_401_ao_listar_sem_token() throws Exception {
        mockMvc.perform(get("/orcamentos").param("mes", "5").param("ano", "2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deve_retornar_lista_vazia_quando_sem_orcamentos() throws Exception {
        mockMvc.perform(get("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .param("mes", "5")
                        .param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deve_retornar_orcamentos_do_mes_do_usuario() throws Exception {
        // Cria 2 orçamentos via API
        mockMvc.perform(post("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody("ALIMENTACAO", 500, 5, 2026))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody("TRANSPORTE", 300, 5, 2026))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .param("mes", "5")
                        .param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // =========================================================================
    // PUT /orcamentos/{id}
    // =========================================================================

    @Test
    void deve_retornar_200_ao_atualizar_orcamento_existente() throws Exception {
        // Cria orçamento para obter o id
        String criarResponse = mockMvc.perform(post("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody("SAUDE", 400, 5, 2026))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(criarResponse).get("id").asLong();

        mockMvc.perform(put("/orcamentos/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody("SAUDE", 800, 5, 2026))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorLimite").value(800.0));
    }

    @Test
    void deve_retornar_404_ao_atualizar_orcamento_inexistente() throws Exception {
        mockMvc.perform(put("/orcamentos/9999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody("LAZER", 200, 5, 2026))))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // DELETE /orcamentos/{id}
    // =========================================================================

    @Test
    void deve_retornar_204_ao_deletar_orcamento_existente() throws Exception {
        String criarResponse = mockMvc.perform(post("/orcamentos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody("EDUCACAO", 300, 5, 2026))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(criarResponse).get("id").asLong();

        mockMvc.perform(delete("/orcamentos/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deve_retornar_404_ao_deletar_orcamento_inexistente() throws Exception {
        mockMvc.perform(delete("/orcamentos/9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // GET /orcamentos/otimizacao
    // =========================================================================

    @Test
    void deve_retornar_401_ao_acessar_otimizacao_sem_token() throws Exception {
        mockMvc.perform(get("/orcamentos/otimizacao").param("mes", "5").param("ano", "2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deve_retornar_200_com_estrutura_vazia_quando_sem_transacoes_e_sem_orcamentos() throws Exception {
        mockMvc.perform(get("/orcamentos/otimizacao")
                        .header("Authorization", "Bearer " + token)
                        .param("mes", "5")
                        .param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gastosAcimaOrcamento").isArray())
                .andExpect(jsonPath("$.gastosAcimaOrcamento.length()").value(0))
                .andExpect(jsonPath("$.economiasPotenciais").value(0))
                .andExpect(jsonPath("$.mes").value(5))
                .andExpect(jsonPath("$.ano").value(2026));
    }
}
