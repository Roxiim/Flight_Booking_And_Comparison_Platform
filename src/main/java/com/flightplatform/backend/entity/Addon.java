package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "addons") @Getter @Setter @NoArgsConstructor
public class Addon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 255) private String description;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Column(length = 3) private String currency;
}
