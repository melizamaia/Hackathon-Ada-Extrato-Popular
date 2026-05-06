package com.extratoPopular.domain.model;

import com.extratoPopular.domain.enums.Categoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "orcamentos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orcamento_user_categoria_mes_ano",
                columnNames = {"user_id", "categoria", "mes", "ano"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valorLimite;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private Integer ano;
}
