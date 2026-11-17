package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Booking;
import com.flightplatform.backend.entity.BookingStatus;
import com.flightplatform.backend.entity.Passenger;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.InvalidOperationException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.BookingRepository;
import com.flightplatform.backend.repository.BookingSeatRepository;
import com.flightplatform.backend.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PassengerService {
    private final PassengerRepository passengerRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public List<Passenger> getByBooking(Long bookingId) {
        if (!bookingRepository.existsById(bookingId)) throw new ResourceNotFoundException("Booking", bookingId);
        return passengerRepository.findByBooking_Id(bookingId);
    }

    @Transactional
    public Passenger create(Long bookingId, Passenger passenger) {
        validate(passenger);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        ensurePending(booking);
        passenger.setId(null);
        passenger.setBooking(booking);
        normalize(passenger);
        return passengerRepository.save(passenger);
    }

    @Transactional
    public Passenger update(Long bookingId, Long passengerId, Passenger changes) {
        validate(changes);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        ensurePending(booking);
        Passenger existing = passengerRepository.findById(passengerId).orElseThrow(() -> new ResourceNotFoundException("Passenger", passengerId));
        if (!existing.getBooking().getId().equals(bookingId)) throw new InvalidOperationException("Passenger does not belong to this booking");
        existing.setFirstName(changes.getFirstName().trim());
        existing.setLastName(changes.getLastName().trim());
        existing.setDateOfBirth(changes.getDateOfBirth());
        existing.setPassportNumber(AirlineService.trimToNull(changes.getPassportNumber()));
        existing.setNationality(AirlineService.trimToNull(changes.getNationality()));
        return passengerRepository.save(existing);
    }

    @Transactional
    public void delete(Long bookingId, Long passengerId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        ensurePending(booking);
        Passenger passenger = passengerRepository.findById(passengerId).orElseThrow(() -> new ResourceNotFoundException("Passenger", passengerId));
        if (!passenger.getBooking().getId().equals(bookingId)) throw new InvalidOperationException("Passenger does not belong to this booking");
        if (bookingSeatRepository.existsByPassenger_Id(passengerId)) throw new InvalidOperationException("Passenger cannot be removed while a seat is assigned");
        passengerRepository.delete(passenger);
    }

    private static void validate(Passenger passenger) {
        if (passenger == null) throw new BusinessRuleException("Passenger is required");
        AirlineService.requireText(passenger.getFirstName(), "Passenger first name");
        AirlineService.requireText(passenger.getLastName(), "Passenger last name");
        if (passenger.getDateOfBirth() == null || passenger.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Passenger date of birth must be a valid date in the past");
        }
    }

    private static void normalize(Passenger passenger) {
        passenger.setFirstName(passenger.getFirstName().trim());
        passenger.setLastName(passenger.getLastName().trim());
        passenger.setPassportNumber(AirlineService.trimToNull(passenger.getPassportNumber()));
        passenger.setNationality(AirlineService.trimToNull(passenger.getNationality()));
    }

    private static void ensurePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) throw new InvalidOperationException("Passengers can only be changed on pending bookings");
    }
}
