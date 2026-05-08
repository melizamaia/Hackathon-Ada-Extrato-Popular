package com.extratoPopular.infrastructure.security;

import com.extratoPopular.domain.exception.UsuarioNaoAutenticadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deve_retornar_user_id_quando_autenticado() {
        var auth = new UsernamePasswordAuthenticationToken(42L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertEquals(42L, SecurityUtils.getCurrentUserId());
    }

    @Test
    void deve_lancar_excecao_quando_contexto_vazio() {
        SecurityContextHolder.clearContext();
        assertThrows(UsuarioNaoAutenticadoException.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void deve_lancar_excecao_quando_principal_e_anonymous() {
        var auth = new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThrows(UsuarioNaoAutenticadoException.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void deve_lancar_excecao_quando_principal_nao_e_long() {
        var auth = new UsernamePasswordAuthenticationToken("nao-e-long", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertThrows(UsuarioNaoAutenticadoException.class, SecurityUtils::getCurrentUserId);
    }

    @Test
    void construtor_deve_lancar_illegal_state() {
        assertThrows(IllegalStateException.class, () -> {
            var ctor = SecurityUtils.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            try {
                ctor.newInstance();
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }
}
