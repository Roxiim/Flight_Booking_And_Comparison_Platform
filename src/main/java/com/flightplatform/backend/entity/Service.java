package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "services") @Getter @Setter @NoArgsConstructor
public class Service {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String name;
    @Column(length = 255) private String description;
    @OneToMany(mappedBy = "service") private List<FlightService> flightServices = new ArrayList<>();
}
