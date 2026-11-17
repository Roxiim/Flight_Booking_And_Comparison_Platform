package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "flights") @Getter @Setter @NoArgsConstructor
public class Flight {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "airline_id", nullable = false) private Airline airline;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "aircraft_id", nullable = false) private Aircraft aircraft;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "origin_airport_id", nullable = false) private Airport originAirport;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "destination_airport_id", nullable = false) private Airport destinationAirport;
    @Column(name = "flight_number", nullable = false, length = 20) private String flightNumber;
    @Column(name = "departure_time", nullable = false) private LocalDateTime departureTime;
    @Column(name = "arrival_time", nullable = false) private LocalDateTime arrivalTime;
    @Column(name = "number_of_stops") private Integer numberOfStops;
    @Enumerated(EnumType.STRING) @Column private FlightStatus status;
    @OneToMany(mappedBy = "flight") private List<FlightFare> fares = new ArrayList<>();
    @OneToMany(mappedBy = "flight") private List<FlightService> flightServices = new ArrayList<>();
}
