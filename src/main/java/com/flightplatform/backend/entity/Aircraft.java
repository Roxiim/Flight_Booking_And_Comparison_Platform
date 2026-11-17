package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aircraft")
@Getter @Setter @NoArgsConstructor
public class Aircraft {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;
    @Column(nullable = false, length = 100)
    private String model;
    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;
    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;
}
