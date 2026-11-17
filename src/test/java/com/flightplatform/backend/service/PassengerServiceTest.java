package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Booking;
import com.flightplatform.backend.entity.BookingStatus;
import com.flightplatform.backend.entity.Passenger;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.InvalidOperationException;
import com.flightplatform.backend.repository.BookingRepository;
import com.flightplatform.backend.repository.BookingSeatRepository;
import com.flightplatform.backend.repository.PassengerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PassengerServiceTest {
    @Mock PassengerRepository passengers;
    @Mock BookingRepository bookings;
    @Mock BookingSeatRepository seats;
    @InjectMocks PassengerService service;

    @Test void createsPassengerForPendingBooking() {
        Booking booking = new Booking(); booking.setId(2L); booking.setStatus(BookingStatus.PENDING);
        Passenger passenger = passenger();
        when(bookings.findByIdForUpdate(2L)).thenReturn(Optional.of(booking));
        when(passengers.save(passenger)).thenReturn(passenger);
        assertSame(passenger, service.create(2L, passenger));
        assertSame(booking, passenger.getBooking());
    }

    @Test void rejectsFutureDateOfBirth() {
        Passenger passenger = passenger(); passenger.setDateOfBirth(LocalDate.now().plusDays(1));
        assertThrows(BusinessRuleException.class, () -> service.create(2L, passenger));
        verifyNoInteractions(bookings);
    }

    @Test void refusesPassengerUpdatesAfterConfirmation() {
        Booking booking = new Booking(); booking.setStatus(BookingStatus.CONFIRMED);
        when(bookings.findByIdForUpdate(2L)).thenReturn(Optional.of(booking));
        assertThrows(InvalidOperationException.class, () -> service.update(2L, 9L, passenger()));
        verify(passengers, never()).findById(any());
    }

    private static Passenger passenger() {
        Passenger passenger = new Passenger(); passenger.setFirstName("Ada"); passenger.setLastName("Lovelace");
        passenger.setDateOfBirth(LocalDate.of(1815, 12, 10)); return passenger;
    }
}
