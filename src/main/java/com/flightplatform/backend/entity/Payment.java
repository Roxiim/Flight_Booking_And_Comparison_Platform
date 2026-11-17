package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "payments") @Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id", nullable = false) private Booking booking;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(name = "payment_method", nullable = false) private PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING) @Column private PaymentStatus status;
    @Column(name = "transaction_reference", unique = true, length = 100) private String transactionReference;
    @Column(name = "payment_date") private LocalDateTime paymentDate;
}
