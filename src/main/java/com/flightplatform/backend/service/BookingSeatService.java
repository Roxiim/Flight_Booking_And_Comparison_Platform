package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.exception.InvalidOperationException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingSeatService {
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingFlightRepository bookingFlightRepository;
    private final BookingRepository bookingRepository;
    private final PassengerRepository passengerRepository;
    private final AircraftSeatRepository aircraftSeatRepository;
    private final FlightRepository flightRepository;
    private final BookingService bookingService;

    public List<AircraftSeat> getAvailableSeats(Long flightId) {
        Flight flight = flightRepository.findById(flightId).orElseThrow(() -> new ResourceNotFoundException("Flight", flightId));
        ensureBookableFlight(flight);
        Set<Long> occupied = bookingSeatRepository.findByFlight_Id(flightId).stream()
                .map(seat -> seat.getAircraftSeat().getId()).collect(Collectors.toSet());
        return aircraftSeatRepository.findByAircraft_Id(flight.getAircraft().getId()).stream()
                .filter(seat -> !occupied.contains(seat.getId())).toList();
    }

    @Transactional
    public BookingSeat reserveSeat(Long bookingFlightId, Long passengerId, Long aircraftSeatId, BigDecimal seatPrice) {
        BookingFlight bookingFlight = bookingFlightRepository.findById(bookingFlightId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking flight", bookingFlightId));
        Flight flight = lockFlight(bookingFlight.getFlight().getId());
        Booking booking = lockPendingBooking(bookingFlight.getBooking().getId());
        Passenger passenger = passengerRepository.findById(passengerId).orElseThrow(() -> new ResourceNotFoundException("Passenger", passengerId));
        AircraftSeat aircraftSeat = aircraftSeatRepository.findById(aircraftSeatId)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft seat", aircraftSeatId));
        validateAssignment(bookingFlight, booking, passenger, flight, aircraftSeat, seatPrice);
        if (bookingSeatRepository.existsByFlight_IdAndAircraftSeat_Id(flight.getId(), aircraftSeatId)) {
            throw new DuplicateResourceException("Seat is already reserved for this flight");
        }
        if (bookingSeatRepository.existsByBookingFlight_IdAndPassenger_Id(bookingFlightId, passengerId)) {
            throw new DuplicateResourceException("Passenger already has a seat for this flight segment");
        }
        BookingSeat seat = new BookingSeat();
        seat.setBookingFlight(bookingFlight);
        seat.setFlight(flight);
        seat.setPassenger(passenger);
        seat.setAircraftSeat(aircraftSeat);
        seat.setSeatPrice(seatPrice == null ? BigDecimal.ZERO : seatPrice);
        BookingSeat saved = bookingSeatRepository.saveAndFlush(seat);
        bookingService.recalculateTotal(booking.getId());
        return saved;
    }

    @Transactional
    public BookingSeat changeSeat(Long bookingFlightId, Long passengerId, Long newAircraftSeatId, BigDecimal seatPrice) {
        BookingFlight bookingFlight = bookingFlightRepository.findById(bookingFlightId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking flight", bookingFlightId));
        Flight flight = lockFlight(bookingFlight.getFlight().getId());
        Booking booking = lockPendingBooking(bookingFlight.getBooking().getId());
        Passenger passenger = passengerRepository.findById(passengerId).orElseThrow(() -> new ResourceNotFoundException("Passenger", passengerId));
        AircraftSeat newSeat = aircraftSeatRepository.findById(newAircraftSeatId)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft seat", newAircraftSeatId));
        BookingSeat existing = bookingSeatRepository.findByBookingFlight_IdAndPassenger_Id(bookingFlightId, passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat assignment for passenger", passengerId));
        validateAssignment(bookingFlight, booking, passenger, flight, newSeat, seatPrice);
        if (!existing.getAircraftSeat().getId().equals(newAircraftSeatId)
                && bookingSeatRepository.existsByFlight_IdAndAircraftSeat_Id(flight.getId(), newAircraftSeatId)) {
            throw new DuplicateResourceException("Seat is already reserved for this flight");
        }
        existing.setAircraftSeat(newSeat);
        existing.setSeatPrice(seatPrice == null ? BigDecimal.ZERO : seatPrice);
        BookingSeat saved = bookingSeatRepository.saveAndFlush(existing);
        bookingService.recalculateTotal(booking.getId());
        return saved;
    }

    @Transactional
    public void releaseSeat(Long bookingSeatId) {
        BookingSeat seat = bookingSeatRepository.findById(bookingSeatId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking seat", bookingSeatId));
        lockFlight(seat.getFlight().getId());
        Booking booking = lockPendingBooking(seat.getBookingFlight().getBooking().getId());
        bookingSeatRepository.delete(seat);
        bookingService.recalculateTotal(booking.getId());
    }

    private Flight lockFlight(Long flightId) {
        Flight flight = flightRepository.findByIdForUpdate(flightId).orElseThrow(() -> new ResourceNotFoundException("Flight", flightId));
        ensureBookableFlight(flight);
        return flight;
    }

    private Booking lockPendingBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (booking.getStatus() != BookingStatus.PENDING) throw new InvalidOperationException("Seats can only be changed on pending bookings");
        return booking;
    }

    private static void ensureBookableFlight(Flight flight) {
        if (flight.getStatus() != FlightStatus.SCHEDULED && flight.getStatus() != FlightStatus.DELAYED) {
            throw new InvalidOperationException("Seats cannot be assigned to an unavailable flight");
        }
    }

    private static void validateAssignment(BookingFlight bookingFlight, Booking booking, Passenger passenger,
            Flight flight, AircraftSeat seat, BigDecimal price) {
        if (!bookingFlight.getBooking().getId().equals(booking.getId()) || !bookingFlight.getFlight().getId().equals(flight.getId())) {
            throw new BusinessRuleException("Booking-flight association is inconsistent");
        }
        if (!passenger.getBooking().getId().equals(booking.getId())) throw new BusinessRuleException("Passenger must belong to the booking");
        if (!seat.getAircraft().getId().equals(flight.getAircraft().getId())) throw new BusinessRuleException("Seat does not belong to the aircraft assigned to this flight");
        if (price != null && price.signum() < 0) throw new BusinessRuleException("Seat price cannot be negative");
    }
}
