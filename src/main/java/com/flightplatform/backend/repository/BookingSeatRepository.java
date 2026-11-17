package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    List<BookingSeat> findByFlight_Id(Long flightId);
    List<BookingSeat> findByBookingFlight_Booking_Id(Long bookingId);
    boolean existsByAircraftSeat_Id(Long aircraftSeatId);
    boolean existsByPassenger_Id(Long passengerId);
    boolean existsByFlight_IdAndAircraftSeat_Id(Long flightId, Long aircraftSeatId);
    boolean existsByBookingFlight_IdAndPassenger_Id(Long bookingFlightId, Long passengerId);
    long countByFlight_Id(Long flightId);
    long deleteByBookingFlight_Booking_Id(Long bookingId);
    Optional<BookingSeat> findByBookingFlight_IdAndPassenger_Id(Long bookingFlightId, Long passengerId);
}
