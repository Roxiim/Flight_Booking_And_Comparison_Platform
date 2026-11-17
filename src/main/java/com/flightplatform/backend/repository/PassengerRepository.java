package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    List<Passenger> findByBooking_Id(Long bookingId);
    long countByBooking_Id(Long bookingId);
}
