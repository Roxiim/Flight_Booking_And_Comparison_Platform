package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "booking_addons") @Getter @Setter @NoArgsConstructor
public class BookingAddon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_flight_id", nullable = false) private BookingFlight bookingFlight;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "passenger_id") private Passenger passenger;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "addon_id", nullable = false) private Addon addon;
    @Column private Integer quantity;
    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2) private BigDecimal priceAtBooking;
}
