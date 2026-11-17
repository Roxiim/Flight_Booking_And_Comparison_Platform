package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.BookingFlight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingFlightRepository extends JpaRepository<BookingFlight, Long> {
    List<BookingFlight> findByBooking_Id(Long bookingId);
    long countByBooking_Id(Long bookingId);
    boolean existsByFlight_Id(Long flightId);
    boolean existsByFare_Id(Long fareId);
}
