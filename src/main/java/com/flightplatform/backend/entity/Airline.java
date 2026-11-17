package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "airlines")
@Getter
@Setter
@NoArgsConstructor
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(length = 255)
    private String website;

    @Column(name = "support_email", length = 150)
    private String supportEmail;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "airline")
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "airline")
    private List<Aircraft> aircraft = new ArrayList<>();

    @OneToMany(mappedBy = "airline")
    private List<Flight> flights = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
