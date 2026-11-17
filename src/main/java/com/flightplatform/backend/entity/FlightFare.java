package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "flight_fares") @Getter @Setter @NoArgsConstructor
public class FlightFare {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flight_id", nullable = false) private Flight flight;
    @Column(name = "fare_name", nullable = false, length = 100) private String fareName;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Column(length = 3) private String currency;
    @Column(name = "personal_item_included") private Boolean personalItemIncluded;
    @Column(name = "cabin_bag_included") private Boolean cabinBagIncluded;
    @Column(name = "checked_bags_included") private Integer checkedBagsIncluded;
    @Column(name = "checked_bag_weight_kg", precision = 5, scale = 2) private BigDecimal checkedBagWeightKg;
    @Column(name = "changes_allowed") private Boolean changesAllowed;
    @Column(name = "refund_allowed") private Boolean refundAllowed;
}
