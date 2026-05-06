package com.extratoPopular.application.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

@Service
public class HashService {

    public String gerarHash(LocalDate data, BigDecimal valor, String descricao) {
        String input = data.toString()
                + valor.toPlainString()
                + descricao.toLowerCase().trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é garantido pela JVM (java.security)
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}
