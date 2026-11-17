package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "booking_flights") @Getter @Setter @NoArgsConstructor
public class BookingFlight {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id", nullable = false) private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flight_id", nullable = false) private Flight flight;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "fare_id", nullable = false) private FlightFare fare;
    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2) private BigDecimal priceAtBooking;
    @OneToMany(mappedBy = "bookingFlight") private List<BookingSeat> seats = new ArrayList<>();
    @OneToMany(mappedBy = "bookingFlight") private List<BookingAddon> addons = new ArrayList<>();
}
