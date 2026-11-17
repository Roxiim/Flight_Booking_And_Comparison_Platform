package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aircraft_seats", uniqueConstraints = @UniqueConstraint(name = "uk_aircraft_seat_number", columnNames = {"aircraft_id", "seat_number"}))
@Getter @Setter @NoArgsConstructor
public class AircraftSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;
    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;
    @Enumerated(EnumType.STRING)
    @Column(name = "cabin_class", nullable = false)
    private CabinClass cabinClass;
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type")
    private SeatType seatType;
}
