package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "bookings") @Getter @Setter @NoArgsConstructor
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "booking_reference", nullable = false, unique = true, length = 20) private String bookingReference;
    @Enumerated(EnumType.STRING) @Column private BookingStatus status;
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2) private BigDecimal totalPrice;
    @Column(length = 3) private String currency;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @OneToMany(mappedBy = "booking") private List<Passenger> passengers = new ArrayList<>();
    @OneToMany(mappedBy = "booking") private List<BookingFlight> bookingFlights = new ArrayList<>();
    @OneToMany(mappedBy = "booking") private List<Payment> payments = new ArrayList<>();
    @PrePersist protected void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
