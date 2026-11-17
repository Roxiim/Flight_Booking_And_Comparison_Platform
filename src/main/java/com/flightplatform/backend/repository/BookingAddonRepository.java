package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.BookingAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingAddonRepository extends JpaRepository<BookingAddon, Long> {
    List<BookingAddon> findByBookingFlight_Id(Long bookingFlightId);
    List<BookingAddon> findByBookingFlight_Booking_Id(Long bookingId);
    boolean existsByAddon_Id(Long addonId);
    Optional<BookingAddon> findByBookingFlight_IdAndAddon_IdAndPassenger_Id(Long bookingFlightId, Long addonId, Long passengerId);
    Optional<BookingAddon> findByBookingFlight_IdAndAddon_IdAndPassengerIsNull(Long bookingFlightId, Long addonId);
}
