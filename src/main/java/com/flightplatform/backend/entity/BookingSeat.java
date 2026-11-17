package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "booking_seats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_booking_seat_flight_seat", columnNames = {"flight_id", "aircraft_seat_id"}),
        @UniqueConstraint(name = "uk_booking_seat_passenger", columnNames = {"booking_flight_id", "passenger_id"})})
@Getter @Setter @NoArgsConstructor
public class BookingSeat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_flight_id", nullable = false) private BookingFlight bookingFlight;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flight_id", nullable = false) private Flight flight;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "passenger_id", nullable = false) private Passenger passenger;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "aircraft_seat_id", nullable = false) private AircraftSeat aircraftSeat;
    @Column(name = "seat_price", precision = 10, scale = 2) private BigDecimal seatPrice;
}
