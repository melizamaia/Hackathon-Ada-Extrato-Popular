package com.extratoPopular.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class HashServiceTest {

    private HashService hashService;

    @BeforeEach
    void setUp() {
        hashService = new HashService();
    }

    @Test
    void deve_gerarMesmoHash_quando_inputIdentico() {
        LocalDate data = LocalDate.of(2024, 6, 15);
        BigDecimal valor = new BigDecimal("150.00");
        String descricao = "IFOOD*RESTAURANTE";

        String hash1 = hashService.gerarHash(data, valor, descricao);
        String hash2 = hashService.gerarHash(data, valor, descricao);

        assertEquals(hash1, hash2);
    }

    @Test
    void deve_gerarHashDiferente_quando_datasDiferentes() {
        BigDecimal valor = new BigDecimal("150.00");
        String descricao = "UBER TRIP";

        String hash1 = hashService.gerarHash(LocalDate.of(2024, 6, 15), valor, descricao);
        String hash2 = hashService.gerarHash(LocalDate.of(2024, 6, 16), valor, descricao);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void deve_gerarHashDiferente_quando_valoresDiferentes() {
        LocalDate data = LocalDate.of(2024, 6, 15);
        String descricao = "MERCADO EXTRA";

        String hash1 = hashService.gerarHash(data, new BigDecimal("100.00"), descricao);
        String hash2 = hashService.gerarHash(data, new BigDecimal("100.01"), descricao);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void deve_gerarHashDiferente_quando_descricoesDiferentes() {
        LocalDate data = LocalDate.of(2024, 6, 15);
        BigDecimal valor = new BigDecimal("50.00");

        String hash1 = hashService.gerarHash(data, valor, "FARMACIA DROGASIL");
        String hash2 = hashService.gerarHash(data, valor, "FARMACIA PACHECO");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void deve_gerarMesmoHash_quando_descricaoDifereCaseEEspacos() {
        LocalDate data = LocalDate.of(2024, 6, 15);
        BigDecimal valor = new BigDecimal("200.00");

        String hash1 = hashService.gerarHash(data, valor, "  PADARIA DO ZÉ  ");
        String hash2 = hashService.gerarHash(data, valor, "  padaria do zé  ");

        assertEquals(hash1, hash2);
    }

    @Test
    void deve_retornarHash_com64Caracteres() {
        String hash = hashService.gerarHash(
                LocalDate.of(2024, 1, 1),
                new BigDecimal("1.00"),
                "qualquer descricao"
        );

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }
}
