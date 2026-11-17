package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long>, JpaSpecificationExecutor<Flight> {
    boolean existsByAirline_Id(Long airlineId);
    boolean existsByAircraft_Id(Long aircraftId);
    List<Flight> findByAirline_Id(Long airlineId);
    List<Flight> findByOriginAirport_IataCodeAndDestinationAirport_IataCodeAndDepartureTimeBetween(
            String originIataCode, String destinationIataCode, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = {"airline", "aircraft", "originAirport", "destinationAirport"})
    java.util.Optional<Flight> findWithDetailsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Flight f where f.id = :id")
    java.util.Optional<Flight> findByIdForUpdate(@Param("id") Long id);
}
