package com.extratoPopular.interfaces.controller;

import com.extratoPopular.domain.model.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransacaoControllerIntegrationTest {

    @Autowired private MockMvc              mockMvc;
    @Autowired private UserRepository       userRepository;
    @Autowired private TransacaoRepository  transacaoRepository;
    @Autowired private JwtService           jwtService;
    @Autowired private PasswordEncoder      passwordEncoder;

    private String token;

    // CSV com 5 linhas válidas — usado como fixture nos cenários 2 e 3
    private static final String CSV_5_LINHAS = """
            data,valor,descricao
            2024-06-01,-150.00,IFOOD RESTAURANTE
            2024-06-02,-50.00,UBER TRIP
            2024-06-03,-320.50,ALUGUEL JUNHO
            2024-06-04,3000.00,SALARIO JUNHO
            2024-06-05,-35.00,FARMACIA DROGASIL
            """;

    @BeforeEach
    void setUp() {
        transacaoRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setNome("Usuário Teste");
        user.setEmail("teste@email.com");
        user.setSenha(passwordEncoder.encode("senha123"));
        user.setRendaMensal(new BigDecimal("3000.00"));
        User salvo = userRepository.save(user);
        token = jwtService.generateToken(salvo.getId());
    }

    @AfterEach
    void tearDown() {
        transacaoRepository.deleteAll();
        userRepository.deleteAll();
    }

    private MockMultipartFile arquivo(String nomeArquivo, String conteudo) {
        return new MockMultipartFile(
                "file",
                nomeArquivo,
                "application/octet-stream",
                conteudo.getBytes(StandardCharsets.UTF_8)
        );
    }

    // =========================================================================
    // Cenário 1 — sem token → 401
    // =========================================================================

    @Test
    void deve_retornar_401_quando_requisicao_sem_token() throws Exception {
        mockMvc.perform(multipart("/transacoes/bulk")
                        .file(arquivo("transacoes.csv", CSV_5_LINHAS)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Cenário 2 — CSV válido com 5 linhas → 201, importadas=5
    // =========================================================================

    @Test
    void deve_retornar_201_e_importadas_5_quando_csv_valido() throws Exception {
        mockMvc.perform(multipart("/transacoes/bulk")
                        .file(arquivo("transacoes.csv", CSV_5_LINHAS))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importadas").value(5))
                .andExpect(jsonPath("$.duplicatas").value(0))
                .andExpect(jsonPath("$.erros").value(0))
                .andExpect(jsonPath("$.transacoes.length()").value(5));
    }

    // =========================================================================
    // Cenário 3 — mesmo CSV duas vezes → importadas=0, duplicatas=5
    // =========================================================================

    @Test
    void deve_retornar_importadas_0_e_duplicatas_5_quando_mesmo_csv_enviado_duas_vezes() throws Exception {
        MockMultipartFile arquivoFixture = arquivo("transacoes.csv", CSV_5_LINHAS);

        // Primeira importação — tudo novo
        mockMvc.perform(multipart("/transacoes/bulk")
                        .file(arquivoFixture)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importadas").value(5));

        // Segunda importação — tudo duplicado
        mockMvc.perform(multipart("/transacoes/bulk")
                        .file(arquivoFixture)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importadas").value(0))
                .andExpect(jsonPath("$.duplicatas").value(5))
                .andExpect(jsonPath("$.erros").value(0));
    }

    // =========================================================================
    // Cenário 4 — 1 linha inválida e 4 válidas → importadas=4, erros=1
    // =========================================================================

    @Test
    void deve_retornar_importadas_4_e_erros_1_quando_csv_com_uma_linha_invalida() throws Exception {
        String csvComErro = """
                data,valor,descricao
                2024-06-01,-150.00,IFOOD RESTAURANTE
                INVALIDA,INVALIDA,LINHA COM ERRO DE FORMATO
                2024-06-03,-320.50,ALUGUEL JUNHO
                2024-06-04,3000.00,SALARIO JUNHO
                2024-06-05,-35.00,FARMACIA DROGASIL
                """;

        mockMvc.perform(multipart("/transacoes/bulk")
                        .file(arquivo("transacoes.csv", csvComErro))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importadas").value(4))
                .andExpect(jsonPath("$.duplicatas").value(0))
                .andExpect(jsonPath("$.erros").value(1));
    }

    // =========================================================================
    // Cenário 5 — arquivo vazio (só header) → 400, erro=ARQUIVO_VAZIO
    // =========================================================================

    @Test
    void deve_retornar_400_e_ARQUIVO_VAZIO_quando_csv_sem_transacoes() throws Exception {
        String csvVazio = "data,valor,descricao\n";

        mockMvc.perform(multipart("/transacoes/bulk")
                        .file(arquivo("vazio.csv", csvVazio))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("ARQUIVO_VAZIO"));
    }

    // =========================================================================
    // Cenário 6 — extensão .txt → 400, erro=FORMATO_INVALIDO
    // =========================================================================

    @Test
    void deve_retornar_400_e_FORMATO_INVALIDO_quando_extensao_nao_suportada() throws Exception {
        mockMvc.perform(multipart("/transacoes/bulk")
                        .file(arquivo("transacoes.txt", CSV_5_LINHAS))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("FORMATO_INVALIDO"));
    }
}
