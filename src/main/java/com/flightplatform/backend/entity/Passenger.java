package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity @Table(name = "passengers") @Getter @Setter @NoArgsConstructor
public class Passenger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id", nullable = false) private Booking booking;
    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name", nullable = false, length = 100) private String lastName;
    @Column(name = "date_of_birth", nullable = false) private LocalDate dateOfBirth;
    @Column(name = "passport_number", length = 50) private String passportNumber;
    @Column(length = 100) private String nationality;
}
