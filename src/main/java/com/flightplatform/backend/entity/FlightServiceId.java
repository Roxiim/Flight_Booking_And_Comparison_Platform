package com.flightplatform.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.Objects;

@Embeddable @Getter @Setter @NoArgsConstructor
public class FlightServiceId implements Serializable {
    @Column(name = "flight_id") private Long flightId;
    @Column(name = "service_id") private Long serviceId;
    public FlightServiceId(Long flightId, Long serviceId) { this.flightId = flightId; this.serviceId = serviceId; }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FlightServiceId that)) return false;
        return Objects.equals(flightId, that.flightId) && Objects.equals(serviceId, that.serviceId);
    }
    @Override public int hashCode() { return Objects.hash(flightId, serviceId); }
}
