package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AirportRepository extends JpaRepository<Airport, Long> {
    Optional<Airport> findByIataCode(String iataCode);
    Optional<Airport> findByIataCodeIgnoreCase(String iataCode);
    boolean existsByIataCodeIgnoreCase(String iataCode);
    List<Airport> findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrCountryContainingIgnoreCase(
            String name, String city, String country);
}
