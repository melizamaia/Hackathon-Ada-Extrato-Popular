package com.extratoPopular.infrastructure.parser;

import com.extratoPopular.application.dto.ParseResult;
import com.extratoPopular.application.dto.TransacaoRaw;
import com.extratoPopular.domain.exception.ArquivoVazioException;
import com.extratoPopular.domain.exception.FormatoArquivoInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvParser {

    private static final Logger log = LoggerFactory.getLogger(CsvParser.class);

    private static final DateTimeFormatter FMT_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_BR  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ParseResult parse(InputStream input) {
        List<TransacaoRaw> transacoes = new ArrayList<>();
        int erros = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                throw new ArquivoVazioException("Arquivo CSV está vazio.");
            }

            String separador = header.contains(";") ? ";" : ",";

            String linha;
            int numeroLinha = 1;

            while ((linha = reader.readLine()) != null) {
                numeroLinha++;

                if (linha.isBlank() || linha.startsWith("#")) {
                    continue;
                }

                try {
                    transacoes.add(parseLinha(linha, separador));
                } catch (Exception e) {
                    log.warn("Linha {} ignorada [{}]: {}", numeroLinha, linha, e.getMessage());
                    erros++;
                }
            }

        } catch (ArquivoVazioException e) {
            throw e;
        } catch (IOException e) {
            throw new FormatoArquivoInvalidoException("Erro ao ler arquivo CSV: " + e.getMessage());
        }

        if (transacoes.isEmpty()) {
            throw new ArquivoVazioException("Nenhuma transação válida encontrada no arquivo CSV.");
        }

        return new ParseResult(transacoes, erros);
    }

    private TransacaoRaw parseLinha(String linha, String separador) {
        String[] partes = linha.split(separador, -1);
        if (partes.length < 3) {
            throw new IllegalArgumentException("esperado 3 campos, encontrado " + partes.length);
        }

        LocalDate data       = parseData(partes[0].trim());
        BigDecimal valor     = parseValor(partes[1].trim());
        String descricao     = partes[2].trim();

        if (descricao.isEmpty()) {
            throw new IllegalArgumentException("descrição vazia");
        }

        return new TransacaoRaw(data, valor, descricao);
    }

    private LocalDate parseData(String texto) {
        try {
            return texto.contains("/")
                    ? LocalDate.parse(texto, FMT_BR)
                    : LocalDate.parse(texto, FMT_ISO);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("data inválida: '" + texto + "'");
        }
    }

    private BigDecimal parseValor(String texto) {
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("valor inválido: '" + texto + "'");
        }
    }
}
