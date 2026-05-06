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

class OfxParserTest {

    private OfxParser ofxParser;

    @BeforeEach
    void setUp() {
        ofxParser = new OfxParser();
    }

    private InputStream stream(String ofx) {
        return new ByteArrayInputStream(ofx.getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Parsing correto
    // -------------------------------------------------------------------------

    @Test
    void deve_parsear_transacao_com_data_YYYYMMDD() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <DTPOSTED>20240615
                <TRNAMT>-150.00
                <MEMO>IFOOD RESTAURANTE
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(1, result.transacoes().size());
        assertEquals(0, result.erros());
        assertEquals(LocalDate.of(2024, 6, 15), result.transacoes().get(0).data());
        assertEquals(new BigDecimal("-150.00"), result.transacoes().get(0).valor());
        assertEquals("IFOOD RESTAURANTE", result.transacoes().get(0).descricao());
    }

    @Test
    void deve_parsear_transacao_com_data_YYYYMMDDHHMMSS() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <DTPOSTED>20240615120000
                <TRNAMT>3000.00
                <MEMO>SALARIO JUNHO
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(LocalDate.of(2024, 6, 15), result.transacoes().get(0).data());
    }

    @Test
    void deve_usar_NAME_quando_MEMO_ausente() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <DTPOSTED>20240615
                <TRNAMT>-80.00
                <NAME>UBER TRIP
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals("UBER TRIP", result.transacoes().get(0).descricao());
    }

    @Test
    void deve_preferir_MEMO_sobre_NAME_quando_ambos_presentes() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <DTPOSTED>20240615
                <TRNAMT>-80.00
                <NAME>FARMACIA NOME
                <MEMO>FARMACIA DROGASIL DESCRICAO COMPLETA
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals("FARMACIA DROGASIL DESCRICAO COMPLETA", result.transacoes().get(0).descricao());
    }

    @Test
    void deve_parsear_valor_negativo() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <DTPOSTED>20240101
                <TRNAMT>-320.50
                <MEMO>ALUGUEL JANEIRO
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(new BigDecimal("-320.50"), result.transacoes().get(0).valor());
    }

    @Test
    void deve_parsear_valor_positivo() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <DTPOSTED>20240501
                <TRNAMT>5000.00
                <MEMO>SALARIO MAIO
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(new BigDecimal("5000.00"), result.transacoes().get(0).valor());
    }

    @Test
    void deve_parsear_multiplos_blocos_STMTTRN() {
        String ofx = """
                <OFX>
                <BANKTRANLIST>
                <STMTTRN>
                <DTPOSTED>20240601
                <TRNAMT>-150.00
                <MEMO>IFOOD PIZZA
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>20240602
                <TRNAMT>-50.00
                <MEMO>UBER TRIP
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>20240603
                <TRNAMT>3000.00
                <MEMO>SALARIO JUNHO
                </STMTTRN>
                </BANKTRANLIST>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(3, result.transacoes().size());
        assertEquals(0, result.erros());
        assertEquals("IFOOD PIZZA",   result.transacoes().get(0).descricao());
        assertEquals("UBER TRIP",     result.transacoes().get(1).descricao());
        assertEquals("SALARIO JUNHO", result.transacoes().get(2).descricao());
    }

    @Test
    void deve_ignorar_tags_extras_no_bloco() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <TRNTYPE>DEBIT
                <DTPOSTED>20240615
                <TRNAMT>-99.00
                <FITID>ABC123
                <CHECKNUM>001
                <MEMO>NETFLIX ASSINATURA
                <SIC>5999
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(1, result.transacoes().size());
        assertEquals("NETFLIX ASSINATURA", result.transacoes().get(0).descricao());
    }

    // -------------------------------------------------------------------------
    // Blocos inválidos — contabilizados em erros
    // -------------------------------------------------------------------------

    @Test
    void deve_ignorar_bloco_sem_DTPOSTED_e_incrementar_erros() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <TRNAMT>-150.00
                <MEMO>SEM DATA — INVALIDO
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>20240615
                <TRNAMT>-80.00
                <MEMO>UBER TRIP VALIDO
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(1, result.transacoes().size());
        assertEquals(1, result.erros());
        assertEquals("UBER TRIP VALIDO", result.transacoes().get(0).descricao());
    }

    @Test
    void deve_ignorar_bloco_sem_TRNAMT_e_incrementar_erros() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <DTPOSTED>20240615
                <MEMO>SEM VALOR — INVALIDO
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>20240616
                <TRNAMT>200.00
                <MEMO>MERCADO VALIDO
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(1, result.transacoes().size());
        assertEquals(1, result.erros());
        assertEquals("MERCADO VALIDO", result.transacoes().get(0).descricao());
    }

    @Test
    void deve_ignorar_bloco_sem_MEMO_nem_NAME_e_incrementar_erros() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <DTPOSTED>20240615
                <TRNAMT>-50.00
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>20240616
                <TRNAMT>-75.00
                <MEMO>FARMACIA DROGASIL
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(1, result.transacoes().size());
        assertEquals(1, result.erros());
    }

    @Test
    void deve_ignorar_bloco_com_data_invalida_e_incrementar_erros() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <DTPOSTED>DATAERRADA
                <TRNAMT>-50.00
                <MEMO>BLOCO INVALIDO
                </STMTTRN>
                <STMTTRN>
                <DTPOSTED>20240616
                <TRNAMT>-75.00
                <MEMO>BLOCO VALIDO
                </STMTTRN>
                </OFX>
                """;

        ParseResult result = ofxParser.parse(stream(ofx));

        assertEquals(1, result.transacoes().size());
        assertEquals(1, result.erros());
        assertEquals("BLOCO VALIDO", result.transacoes().get(0).descricao());
    }

    // -------------------------------------------------------------------------
    // ArquivoVazioException
    // -------------------------------------------------------------------------

    @Test
    void deve_lancar_ArquivoVazioException_quando_sem_blocos_STMTTRN() {
        String ofx = """
                <OFX>
                <BANKMSGSRSV1>
                <STMTTRNRS>
                </STMTTRNRS>
                </BANKMSGSRSV1>
                </OFX>
                """;

        assertThrows(ArquivoVazioException.class, () -> ofxParser.parse(stream(ofx)));
    }

    @Test
    void deve_lancar_ArquivoVazioException_quando_arquivo_vazio() {
        assertThrows(ArquivoVazioException.class, () -> ofxParser.parse(stream("")));
    }

    @Test
    void deve_lancar_ArquivoVazioException_quando_todos_blocos_invalidos() {
        String ofx = """
                <OFX>
                <STMTTRN>
                <TRNAMT>-50.00
                <MEMO>SEM DATA</STMTTRN>
                <STMTTRN>
                <DTPOSTED>20240615
                <MEMO>SEM VALOR</STMTTRN>
                </OFX>
                """;

        assertThrows(ArquivoVazioException.class, () -> ofxParser.parse(stream(ofx)));
    }
}
