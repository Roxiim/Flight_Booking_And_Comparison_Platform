package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.Airline;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AirlineRepository extends JpaRepository<Airline, Long> {
    Optional<Airline> findByCode(String code);
    Optional<Airline> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Airline> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);
}
