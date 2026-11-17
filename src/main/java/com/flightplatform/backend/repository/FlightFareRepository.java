package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.FlightFare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlightFareRepository extends JpaRepository<FlightFare, Long> {
    List<FlightFare> findByFlight_Id(Long flightId);
    boolean existsByFlight_Id(Long flightId);
}
