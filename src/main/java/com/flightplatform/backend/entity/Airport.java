package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "airports")
@Getter @Setter @NoArgsConstructor
public class Airport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(name = "iata_code", nullable = false, unique = true, length = 3)
    private String iataCode;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(nullable = false, length = 100)
    private String country;
    @Column(length = 100)
    private String timezone;
}
