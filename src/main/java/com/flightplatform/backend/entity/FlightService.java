package com.flightplatform.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "flight_services") @Getter @Setter @NoArgsConstructor
public class FlightService {
    @EmbeddedId private FlightServiceId id = new FlightServiceId();
    @MapsId("flightId") @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flight_id", nullable = false) private Flight flight;
    @MapsId("serviceId") @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_id", nullable = false) private Service service;
}
