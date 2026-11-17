package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.InvalidOperationException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookings;
    @Mock UserRepository users;
    @Mock FlightFareRepository fares;
    @Mock BookingFlightRepository bookingFlights;
    @Mock BookingSeatRepository bookingSeats;
    @Mock BookingAddonRepository bookingAddons;
    @Mock PaymentRepository payments;
    @InjectMocks BookingService service;

    @Test void createsPendingBookingAndSnapshotsFarePrice() {
        User user = new User(); user.setId(7L);
        Flight flight = flight(4L);
        FlightFare fare = fare(10L, flight, "125.50");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(fares.findAllById(List.of(10L))).thenReturn(List.of(fare));
        when(bookings.existsByBookingReference(any())).thenReturn(false);
        when(bookings.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking saved = invocation.getArgument(0); saved.setId(12L); return saved;
        });
        when(bookingFlights.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = service.createBooking(7L, List.of(10L), null);

        assertEquals(BookingStatus.PENDING, booking.getStatus());
        assertEquals("EUR", booking.getCurrency());
        assertEquals(new BigDecimal("125.50"), booking.getTotalPrice());
        verify(bookingFlights).saveAll(argThat(rows -> ((List<?>) rows).size() == 1));
    }

    @Test void rejectsFareIdsThatDoNotExist() {
        when(users.findById(7L)).thenReturn(Optional.of(new User()));
        when(fares.findAllById(List.of(10L))).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> service.createBooking(7L, List.of(10L), null));
        verify(bookings, never()).save(any());
    }

    @Test void rejectsDuplicateFareSelection() {
        assertThrows(BusinessRuleException.class, () -> service.createBooking(7L, List.of(10L, 10L), null));
        verifyNoInteractions(users);
    }

    @Test void cancellationReleasesSeatsAndUpdatesPendingStatus() {
        Booking booking = new Booking(); booking.setId(12L); booking.setStatus(BookingStatus.PENDING);
        when(bookings.findByIdForUpdate(12L)).thenReturn(Optional.of(booking));
        when(bookings.save(booking)).thenReturn(booking);
        assertEquals(BookingStatus.CANCELLED, service.updateStatus(12L, BookingStatus.CANCELLED).getStatus());
        verify(bookingSeats).deleteByBookingFlight_Booking_Id(12L);
    }

    @Test void confirmedBookingCannotBeCancelledByPendingTransition() {
        Booking booking = new Booking(); booking.setStatus(BookingStatus.CONFIRMED);
        when(bookings.findByIdForUpdate(12L)).thenReturn(Optional.of(booking));
        assertThrows(InvalidOperationException.class, () -> service.cancel(12L));
        verify(bookingSeats, never()).deleteByBookingFlight_Booking_Id(any());
    }

    @Test void refundableConfirmedBookingIsCancelledAndPaymentRefunded() {
        Booking booking = new Booking(); booking.setId(12L); booking.setStatus(BookingStatus.CONFIRMED);
        FlightFare fare = new FlightFare(); fare.setRefundAllowed(true);
        BookingFlight leg = new BookingFlight(); leg.setFare(fare);
        Payment payment = new Payment(); payment.setStatus(PaymentStatus.COMPLETED);
        when(bookings.findByIdForUpdate(12L)).thenReturn(Optional.of(booking));
        when(bookingFlights.findByBooking_Id(12L)).thenReturn(List.of(leg));
        when(payments.findByBooking_Id(12L)).thenReturn(List.of(payment));
        when(bookings.save(booking)).thenReturn(booking);
        service.cancel(12L);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(bookingSeats).deleteByBookingFlight_Booking_Id(12L);
    }

    private static Flight flight(Long id) {
        Flight flight = new Flight(); flight.setId(id); flight.setStatus(FlightStatus.SCHEDULED); return flight;
    }
    private static FlightFare fare(Long id, Flight flight, String price) {
        FlightFare fare = new FlightFare(); fare.setId(id); fare.setFlight(flight);
        fare.setPrice(new BigDecimal(price)); fare.setCurrency("EUR"); return fare;
    }
}
