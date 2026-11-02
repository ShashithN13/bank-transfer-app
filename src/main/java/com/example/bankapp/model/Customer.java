package com.example.bankapp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    @NotBlank
    @Email
    private String email;

    @Column(nullable = false, precision = 19, scale = 2)
    @DecimalMin(value = "0.00")
    private BigDecimal balance;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
