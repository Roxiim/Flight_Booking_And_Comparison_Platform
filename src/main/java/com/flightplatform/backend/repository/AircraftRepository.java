package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    boolean existsByAirline_Id(Long airlineId);
    List<Aircraft> findByAirline_Id(Long airlineId);
    Optional<Aircraft> findByRegistrationNumber(String registrationNumber);
}
