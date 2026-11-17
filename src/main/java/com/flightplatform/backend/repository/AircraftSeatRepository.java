package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.AircraftSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AircraftSeatRepository extends JpaRepository<AircraftSeat, Long> {
    List<AircraftSeat> findByAircraft_Id(Long aircraftId);
    boolean existsByAircraft_IdAndSeatNumberIgnoreCase(Long aircraftId, String seatNumber);
    long countByAircraft_Id(Long aircraftId);
}
