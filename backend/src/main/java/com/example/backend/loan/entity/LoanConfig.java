package com.example.backend.loan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "loan_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanConfig {

    @Id
    @Column(name = "config_key", length = 50)
    private String key;

    @NotNull
    @Column(name = "config_value", nullable = false)
    private String value;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}