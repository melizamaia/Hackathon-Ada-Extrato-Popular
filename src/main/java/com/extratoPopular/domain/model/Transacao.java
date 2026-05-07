package com.extratoPopular.domain.model;

import com.extratoPopular.domain.enums.Categoria;
import com.extratoPopular.domain.enums.FonteImportacao;
import com.extratoPopular.domain.enums.TipoTransacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "transacoes",
    uniqueConstraints = @UniqueConstraint(name = "uk_transacao_user_hash", columnNames = {"user_id", "hash"}),
    indexes = @Index(name = "idx_transacao_user_id", columnList = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FonteImportacao fonte;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    private void prePersist() {
        criadoEm = LocalDateTime.now();
    }
}
