package com.extratoPopular.interfaces.controller;

import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.OrcamentoRepository;
import com.extratoPopular.infrastructure.persistence.TransacaoRepository;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.infrastructure.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnaliseFinanceiraControllerIntegrationTest {

    @Autowired private MockMvc             mockMvc;
    @Autowired private UserRepository      userRepository;
    @Autowired private TransacaoRepository transacaoRepository;
    @Autowired private OrcamentoRepository orcamentoRepository;
    @Autowired private JwtService          jwtService;
    @Autowired private PasswordEncoder     passwordEncoder;

    private String token;

    // CSV com: 1 crédito de 3000, 3 débitos (150+80+250 = 480)
    private static final String CSV_FIXTURE = """
            data,valor,descricao
            2026-05-01,3000.00,SALARIO MAIO
            2026-05-05,-150.00,IFOOD RESTAURANTE
            2026-05-10,-80.00,UBER TRIP
            2026-05-15,-250.00,ALUGUEL MAIO
            """;

    @BeforeEach
    void setUp() throws Exception {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setNome("Usuário Analise");
        user.setEmail("analise@email.com");
        user.setSenha(passwordEncoder.encode("senha123"));
        user.setRendaMensal(new BigDecimal("3000.00"));
        User salvo = userRepository.save(user);
        token = jwtService.generateToken(salvo.getId());

        // Importa as transações fixture
        MockMultipartFile arquivo = new MockMultipartFile(
                "file", "transacoes.csv", "application/octet-stream",
                CSV_FIXTURE.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/transacoes/bulk")
                        .file(arquivo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    @AfterEach
    void tearDown() {
        transacaoRepository.deleteAll();
        orcamentoRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // GET /transacoes/resumo
    // =========================================================================

    @Test
    void deve_retornar_401_ao_acessar_resumo_sem_token() throws Exception {
        mockMvc.perform(get("/transacoes/resumo").param("mes", "5").param("ano", "2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deve_retornar_200_com_totais_corretos_para_o_mes() throws Exception {
        mockMvc.perform(get("/transacoes/resumo")
                        .header("Authorization", "Bearer " + token)
                        .param("mes", "5")
                        .param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(5))
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.totalReceitas").value(3000.00))
                .andExpect(jsonPath("$.totalDespesas").value(480.00))
                .andExpect(jsonPath("$.saldo").value(2520.00))
                .andExpect(jsonPath("$.totalTransacoes").value(4));
    }

    @Test
    void deve_retornar_gastos_por_categoria_no_resumo() throws Exception {
        mockMvc.perform(get("/transacoes/resumo")
                        .header("Authorization", "Bearer " + token)
                        .param("mes", "5")
                        .param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gastosPorCategoria").exists())
                .andExpect(jsonPath("$.gastosPorCategoria.ALIMENTACAO").value(150.00))
                .andExpect(jsonPath("$.gastosPorCategoria.TRANSPORTE").value(80.00));
    }

    @Test
    void deve_retornar_zeros_para_mes_sem_transacoes() throws Exception {
        mockMvc.perform(get("/transacoes/resumo")
                        .header("Authorization", "Bearer " + token)
                        .param("mes", "1")
                        .param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas").value(0))
                .andExpect(jsonPath("$.totalDespesas").value(0))
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.totalTransacoes").value(0));
    }

    @Test
    void deve_retornar_lista_de_alertas_no_resumo() throws Exception {
        mockMvc.perform(get("/transacoes/resumo")
                        .header("Authorization", "Bearer " + token)
                        .param("mes", "5")
                        .param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertas").isArray());
    }

    // =========================================================================
    // GET /transacoes/insights
    // =========================================================================

    @Test
    void deve_retornar_401_ao_acessar_insights_sem_token() throws Exception {
        mockMvc.perform(get("/transacoes/insights"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deve_retornar_200_com_campos_de_insights() throws Exception {
        mockMvc.perform(get("/transacoes/insights")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaComMaiorGasto").exists())
                .andExpect(jsonPath("$.percentualRendaComprometida").exists())
                .andExpect(jsonPath("$.mediaGastoPorTransacao").exists())
                .andExpect(jsonPath("$.topCategorias").isArray());
    }

    @Test
    void deve_identificar_moradia_como_maior_gasto() throws Exception {
        // ALUGUEL MAIO (250) > IFOOD (150) > UBER (80) → MORADIA é o maior
        mockMvc.perform(get("/transacoes/insights")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoriaComMaiorGasto").value("MORADIA"));
    }

    @Test
    void deve_calcular_percentual_renda_comprometida() throws Exception {
        // Total despesas: 480 / rendaMensal: 3000 = 16%
        mockMvc.perform(get("/transacoes/insights")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.percentualRendaComprometida").value(16.00));
    }
}
