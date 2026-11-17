package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.FlightService;
import com.flightplatform.backend.entity.FlightServiceId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlightServiceRepository extends JpaRepository<FlightService, FlightServiceId> {
    List<FlightService> findByFlight_Id(Long flightId);
    boolean existsByFlight_IdAndService_Id(Long flightId, Long serviceId);
    boolean existsByFlight_Id(Long flightId);
    boolean existsByService_Id(Long serviceId);
}
