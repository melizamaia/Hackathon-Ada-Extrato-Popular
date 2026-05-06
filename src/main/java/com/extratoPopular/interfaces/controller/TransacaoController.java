package com.extratoPopular.interfaces.controller;

import com.extratoPopular.application.dto.ParseResult;
import com.extratoPopular.application.dto.TransacaoRaw;
import com.extratoPopular.application.usecase.IngestaoTransacoesUseCase;
import com.extratoPopular.domain.enums.FonteImportacao;
import com.extratoPopular.domain.exception.FormatoArquivoInvalidoException;
import com.extratoPopular.infrastructure.parser.CsvParser;
import com.extratoPopular.infrastructure.parser.OfxParser;
import com.extratoPopular.infrastructure.security.SecurityUtils;
import com.extratoPopular.interfaces.dto.BulkImportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/transacoes")
@Tag(name = "Transações", description = "Endpoints para importação de transações financeiras")
public class TransacaoController {

    private final CsvParser csvParser;
    private final OfxParser ofxParser;
    private final IngestaoTransacoesUseCase ingestaoUseCase;

    public TransacaoController(CsvParser csvParser,
                               OfxParser ofxParser,
                               IngestaoTransacoesUseCase ingestaoUseCase) {
        this.csvParser     = csvParser;
        this.ofxParser     = ofxParser;
        this.ingestaoUseCase = ingestaoUseCase;
    }

    @Operation(summary = "Importar transações em lote via arquivo CSV ou OFX/QFX")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Importação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo vazio ou formato não suportado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content)
    })
    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkImportResponse> bulk(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();

        String filename = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase();

        ParseResult parseResult;
        FonteImportacao fonte;

        try {
            InputStream stream = file.getInputStream();
            if (filename.endsWith(".csv")) {
                parseResult = csvParser.parse(stream);
                fonte       = FonteImportacao.CSV;
            } else if (filename.endsWith(".ofx") || filename.endsWith(".qfx")) {
                parseResult = ofxParser.parse(stream);
                fonte       = FonteImportacao.OFX;
            } else {
                throw new FormatoArquivoInvalidoException(
                        "Formato não suportado: '" + file.getOriginalFilename() + "'. Use .csv, .ofx ou .qfx");
            }
        } catch (IOException e) {
            throw new FormatoArquivoInvalidoException("Não foi possível ler o arquivo: " + e.getMessage());
        }

        BulkImportResponse response = ingestaoUseCase.execute(
                userId, parseResult.transacoes(), fonte, parseResult.erros());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
