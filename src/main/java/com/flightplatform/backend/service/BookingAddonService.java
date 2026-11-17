package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.InvalidOperationException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingAddonService {
    private final BookingAddonRepository bookingAddonRepository;
    private final BookingFlightRepository bookingFlightRepository;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final AddonRepository addonRepository;
    private final BookingService bookingService;

    public List<BookingAddon> getByBookingFlight(Long bookingFlightId) {
        if (!bookingFlightRepository.existsById(bookingFlightId)) throw new ResourceNotFoundException("Booking flight", bookingFlightId);
        return bookingAddonRepository.findByBookingFlight_Id(bookingFlightId);
    }

    @Transactional
    public BookingAddon add(Long bookingFlightId, Long addonId, Long passengerId, Integer quantity) {
        if (quantity == null || quantity <= 0) throw new BusinessRuleException("Add-on quantity must be positive");
        BookingFlight bookingFlight = bookingFlightRepository.findById(bookingFlightId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking flight", bookingFlightId));
        Booking booking = bookingRepository.findByIdForUpdate(bookingFlight.getBooking().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingFlight.getBooking().getId()));
        ensurePending(booking);
        Addon addon = addonRepository.findById(addonId).orElseThrow(() -> new ResourceNotFoundException("Add-on", addonId));
        if (!normalizeCurrency(addon.getCurrency()).equals(normalizeCurrency(booking.getCurrency()))) {
            throw new BusinessRuleException("Add-on currency must match booking currency");
        }
        Passenger passenger = null;
        if (passengerId != null) {
            passenger = passengerRepository.findById(passengerId).orElseThrow(() -> new ResourceNotFoundException("Passenger", passengerId));
            if (!passenger.getBooking().getId().equals(booking.getId())) throw new BusinessRuleException("Passenger must belong to this booking");
        }
        boolean duplicate = passenger == null
                ? bookingAddonRepository.findByBookingFlight_IdAndAddon_IdAndPassengerIsNull(bookingFlightId, addonId).isPresent()
                : bookingAddonRepository.findByBookingFlight_IdAndAddon_IdAndPassenger_Id(bookingFlightId, addonId, passengerId).isPresent();
        if (duplicate) throw new InvalidOperationException("This add-on is already assigned to the selected booking passenger/flight");

        BookingAddon line = new BookingAddon();
        line.setBookingFlight(bookingFlight);
        line.setPassenger(passenger);
        line.setAddon(addon);
        line.setQuantity(quantity);
        line.setPriceAtBooking(addon.getPrice());
        BookingAddon saved = bookingAddonRepository.save(line);
        bookingService.recalculateTotal(booking.getId());
        return saved;
    }

    @Transactional
    public void remove(Long bookingId, Long bookingAddonId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        ensurePending(booking);
        BookingAddon line = bookingAddonRepository.findById(bookingAddonId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking add-on", bookingAddonId));
        if (!line.getBookingFlight().getBooking().getId().equals(bookingId)) throw new InvalidOperationException("Add-on does not belong to this booking");
        bookingAddonRepository.delete(line);
        bookingService.recalculateTotal(bookingId);
    }

    private static void ensurePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) throw new InvalidOperationException("Add-ons can only be changed on pending bookings");
    }
    private static String normalizeCurrency(String value) { return value == null || value.isBlank() ? "EUR" : value.trim().toUpperCase(); }
}
