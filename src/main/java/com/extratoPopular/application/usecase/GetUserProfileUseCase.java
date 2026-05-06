package com.extratoPopular.application.usecase;

import com.extratoPopular.domain.exception.UsuarioNaoAutenticadoException;
import com.extratoPopular.domain.model.User;
import com.extratoPopular.infrastructure.persistence.UserRepository;
import com.extratoPopular.infrastructure.security.SecurityUtils;
import com.extratoPopular.interfaces.dto.UserProfileResponse;
import org.springframework.stereotype.Service;

@Service
public class GetUserProfileUseCase {

    private final UserRepository userRepository;

    public GetUserProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse execute() {
        Long userId = SecurityUtils.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsuarioNaoAutenticadoException("Usuário não encontrado na base de dados."));

        return new UserProfileResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getRendaMensal()
        );
    }
}
