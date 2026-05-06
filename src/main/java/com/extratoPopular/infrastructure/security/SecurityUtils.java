package com.extratoPopular.infrastructure.security;

import com.extratoPopular.domain.exception.UsuarioNaoAutenticadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
        // Esconde o construtor público implícito
        throw new IllegalStateException("Classe Utilitária");
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UsuarioNaoAutenticadoException("Usuário não autenticado no contexto de segurança.");
        }

        try {
            return (Long) authentication.getPrincipal();
        } catch (ClassCastException e) {
            throw new UsuarioNaoAutenticadoException("Não foi possível extrair o user_id do contexto de segurança.");
        }
    }
}
