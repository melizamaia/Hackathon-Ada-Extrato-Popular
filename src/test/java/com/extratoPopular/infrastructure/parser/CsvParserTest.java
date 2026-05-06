package com.extratoPopular.infrastructure.parser;

import com.extratoPopular.application.dto.ParseResult;
import com.extratoPopular.domain.exception.ArquivoVazioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    private CsvParser csvParser;

    @BeforeEach
    void setUp() {
        csvParser = new CsvParser();
    }

    private InputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Parsing correto
    // -------------------------------------------------------------------------

    @Test
    void deve_parsear_linha_valida_com_separador_virgula() {
        String csv = "data,valor,descricao\n2024-06-15,150.00,IFOOD RESTAURANTE\n";

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(1, result.transacoes().size());
        assertEquals(0, result.erros());
        assertEquals(LocalDate.of(2024, 6, 15), result.transacoes().get(0).data());
        assertEquals(new BigDecimal("150.00"), result.transacoes().get(0).valor());
        assertEquals("IFOOD RESTAURANTE", result.transacoes().get(0).descricao());
    }

    @Test
    void deve_parsear_linha_valida_com_separador_ponto_e_virgula() {
        String csv = "data;valor;descricao\n2024-06-15;-50.00;UBER TRIP\n";

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(1, result.transacoes().size());
        assertEquals(new BigDecimal("-50.00"), result.transacoes().get(0).valor());
        assertEquals("UBER TRIP", result.transacoes().get(0).descricao());
    }

    @Test
    void deve_parsear_data_no_formato_brasileiro() {
        String csv = "data,valor,descricao\n15/06/2024,200.00,FARMACIA DROGASIL\n";

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(LocalDate.of(2024, 6, 15), result.transacoes().get(0).data());
    }

    @Test
    void deve_parsear_valor_negativo() {
        String csv = "data,valor,descricao\n2024-01-10,-320.50,ALUGUEL JANEIRO\n";

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(new BigDecimal("-320.50"), result.transacoes().get(0).valor());
    }

    @Test
    void deve_parsear_multiplas_linhas_validas() {
        String csv = """
                data,valor,descricao
                2024-06-01,100.00,MERCADO EXTRA
                2024-06-02,-50.00,UBER TRIP
                2024-06-03,3000.00,SALARIO JUNHO
                """;

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(3, result.transacoes().size());
        assertEquals(0, result.erros());
    }

    // -------------------------------------------------------------------------
    // Linhas ignoradas — contabilizadas em erros
    // -------------------------------------------------------------------------

    @Test
    void deve_ignorar_linhas_em_branco_sem_contar_como_erro() {
        String csv = "data,valor,descricao\n\n2024-06-15,150.00,PADARIA ZE\n\n";

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(1, result.transacoes().size());
        assertEquals(0, result.erros());
    }

    @Test
    void deve_ignorar_linhas_de_comentario_sem_contar_como_erro() {
        String csv = "data,valor,descricao\n# exportado em 2024-06-15\n2024-06-15,75.00,SPOTIFY\n";

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(1, result.transacoes().size());
        assertEquals("SPOTIFY", result.transacoes().get(0).descricao());
        assertEquals(0, result.erros());
    }

    @Test
    void deve_ignorar_linha_com_data_invalida_e_incrementar_erros() {
        String csv = """
                data,valor,descricao
                NAO-E-DATA,150.00,IFOOD
                2024-06-15,200.00,MERCADO
                """;

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(1, result.transacoes().size());
        assertEquals(1, result.erros());
        assertEquals("MERCADO", result.transacoes().get(0).descricao());
    }

    @Test
    void deve_ignorar_linha_com_valor_invalido_e_incrementar_erros() {
        String csv = """
                data,valor,descricao
                2024-06-15,ABCD,UBER
                2024-06-16,99.90,FARMACIA
                """;

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(1, result.transacoes().size());
        assertEquals(1, result.erros());
        assertEquals("FARMACIA", result.transacoes().get(0).descricao());
    }

    @Test
    void deve_ignorar_linha_com_menos_de_tres_campos_e_incrementar_erros() {
        String csv = """
                data,valor,descricao
                2024-06-15,150.00
                2024-06-16,200.00,VALIDA
                """;

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(1, result.transacoes().size());
        assertEquals(1, result.erros());
    }

    @Test
    void deve_ignorar_linha_com_descricao_vazia_e_incrementar_erros() {
        String csv = """
                data,valor,descricao
                2024-06-15,150.00,
                2024-06-16,200.00,NETFLIX
                """;

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(1, result.transacoes().size());
        assertEquals(1, result.erros());
    }

    @Test
    void deve_acumular_multiplos_erros_de_parse() {
        String csv = """
                data,valor,descricao
                INVALIDO,150.00,DESC1
                2024-06-15,INVALIDO,DESC2
                2024-06-15,100.00,VALIDA
                """;

        ParseResult result = csvParser.parse(stream(csv));

        assertEquals(1, result.transacoes().size());
        assertEquals(2, result.erros());
    }

    // -------------------------------------------------------------------------
    // ArquivoVazioException
    // -------------------------------------------------------------------------

    @Test
    void deve_lancar_ArquivoVazioException_quando_inputVazio() {
        assertThrows(ArquivoVazioException.class, () -> csvParser.parse(stream("")));
    }

    @Test
    void deve_lancar_ArquivoVazioException_quando_apenas_header() {
        String csv = "data,valor,descricao\n";

        assertThrows(ArquivoVazioException.class, () -> csvParser.parse(stream(csv)));
    }

    @Test
    void deve_lancar_ArquivoVazioException_quando_todas_linhas_invalidas() {
        String csv = """
                data,valor,descricao
                INVALIDO,INVALIDO,
                ,INVALIDO,DESCRICAO
                """;

        assertThrows(ArquivoVazioException.class, () -> csvParser.parse(stream(csv)));
    }

    @Test
    void deve_lancar_ArquivoVazioException_quando_apenas_comentarios_e_brancos() {
        String csv = """
                data,valor,descricao
                # apenas comentario

                """;

        assertThrows(ArquivoVazioException.class, () -> csvParser.parse(stream(csv)));
    }
}
