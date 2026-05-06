package com.extratoPopular.infrastructure.parser;

import com.extratoPopular.application.dto.ParseResult;
import com.extratoPopular.application.dto.TransacaoRaw;
import com.extratoPopular.domain.exception.ArquivoVazioException;
import com.extratoPopular.domain.exception.FormatoArquivoInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OfxParser {

    private static final Logger log = LoggerFactory.getLogger(OfxParser.class);

    private static final DateTimeFormatter FMT_OFX = DateTimeFormatter.ofPattern("yyyyMMdd");

    // Bloco completo de transação OFX
    private static final Pattern BLOCO_STMTTRN = Pattern.compile(
            "<STMTTRN>(.+?)</STMTTRN>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    // Tag folha SGML: <NOME>valor — sem tag de fechamento
    private static final Pattern TAG_FOLHA = Pattern.compile(
            "<([A-Z]+)>([^<\\r\\n]+)",
            Pattern.CASE_INSENSITIVE
    );

    public ParseResult parse(InputStream input) {
        String conteudo = lerConteudo(input);
        List<TransacaoRaw> transacoes = new ArrayList<>();
        int erros = 0;

        Matcher blocos = BLOCO_STMTTRN.matcher(conteudo);
        int blocoNumero = 0;

        while (blocos.find()) {
            blocoNumero++;
            try {
                transacoes.add(parseBloco(blocos.group(1)));
            } catch (Exception e) {
                log.warn("Bloco STMTTRN #{} ignorado: {}", blocoNumero, e.getMessage());
                erros++;
            }
        }

        if (transacoes.isEmpty()) {
            throw new ArquivoVazioException("Nenhuma transação válida encontrada no arquivo OFX.");
        }

        return new ParseResult(transacoes, erros);
    }

    private TransacaoRaw parseBloco(String bloco) {
        Map<String, String> campos = extrairCampos(bloco);

        String dtPosted = campos.get("DTPOSTED");
        if (dtPosted == null) {
            throw new IllegalArgumentException("DTPOSTED ausente");
        }

        String trnAmt = campos.get("TRNAMT");
        if (trnAmt == null) {
            throw new IllegalArgumentException("TRNAMT ausente");
        }

        // MEMO tem precedência sobre NAME
        String memo = campos.get("MEMO");
        String descricao = (memo != null && !memo.isBlank()) ? memo : campos.get("NAME");
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("MEMO e NAME ausentes ou vazios");
        }

        LocalDate data   = parseData(dtPosted.trim());
        BigDecimal valor = parseValor(trnAmt.trim());

        return new TransacaoRaw(data, valor, descricao.trim());
    }

    private Map<String, String> extrairCampos(String bloco) {
        Map<String, String> campos = new HashMap<>();
        Matcher m = TAG_FOLHA.matcher(bloco);
        while (m.find()) {
            // primeira ocorrência de cada tag vence (comportamento padrão OFX)
            campos.putIfAbsent(m.group(1).toUpperCase(), m.group(2).trim());
        }
        return campos;
    }

    private LocalDate parseData(String texto) {
        // Aceita YYYYMMDD ou YYYYMMDDHHMMSS — usa apenas os 8 primeiros dígitos
        String parte = texto.length() >= 8 ? texto.substring(0, 8) : texto;
        try {
            return LocalDate.parse(parte, FMT_OFX);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("data OFX inválida: '" + texto + "'");
        }
    }

    private BigDecimal parseValor(String texto) {
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor OFX inválido: '" + texto + "'");
        }
    }

    private String lerConteudo(InputStream input) {
        try {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FormatoArquivoInvalidoException("Erro ao ler arquivo OFX: " + e.getMessage());
        }
    }
}
