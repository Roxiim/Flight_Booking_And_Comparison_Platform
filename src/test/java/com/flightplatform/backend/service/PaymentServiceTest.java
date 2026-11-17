package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.exception.InvalidOperationException;
import com.flightplatform.backend.repository.BookingRepository;
import com.flightplatform.backend.repository.BookingFlightRepository;
import com.flightplatform.backend.repository.PassengerRepository;
import com.flightplatform.backend.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository payments;
    @Mock BookingRepository bookings;
    @Mock BookingFlightRepository bookingFlights;
    @Mock PassengerRepository passengers;
    @InjectMocks PaymentService service;

    @Test void createsPendingPaymentForBookingTotal() {
        Booking booking = booking();
        when(bookings.findByIdForUpdate(6L)).thenReturn(Optional.of(booking));
        when(bookingFlights.countByBooking_Id(6L)).thenReturn(1L);
        when(passengers.countByBooking_Id(6L)).thenReturn(1L);
        when(payments.existsByBooking_IdAndStatus(6L, PaymentStatus.PENDING)).thenReturn(false);
        when(payments.existsByBooking_IdAndStatus(6L, PaymentStatus.COMPLETED)).thenReturn(false);
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Payment result = service.createPayment(6L, PaymentMethod.CARD);
        assertEquals(new BigDecimal("88.25"), result.getAmount());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertTrue(result.getTransactionReference().startsWith("SIM-"));
    }

    @Test void successfulSimulationConfirmsBooking() {
        Booking booking = booking();
        Payment payment = new Payment(); payment.setId(9L); payment.setBooking(booking); payment.setStatus(PaymentStatus.PENDING);
        when(payments.findByIdForUpdate(9L)).thenReturn(Optional.of(payment));
        when(bookings.findByIdForUpdate(6L)).thenReturn(Optional.of(booking));
        when(payments.existsByBooking_IdAndStatus(6L, PaymentStatus.COMPLETED)).thenReturn(false);
        when(payments.save(payment)).thenReturn(payment);
        when(bookings.save(booking)).thenReturn(booking);
        assertEquals(PaymentStatus.COMPLETED, service.processSimulatedPayment(9L, true).getStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertNotNull(payment.getPaymentDate());
    }

    @Test void failedSimulationKeepsBookingPending() {
        Booking booking = booking();
        Payment payment = new Payment(); payment.setId(9L); payment.setBooking(booking); payment.setStatus(PaymentStatus.PENDING);
        when(payments.findByIdForUpdate(9L)).thenReturn(Optional.of(payment));
        when(bookings.findByIdForUpdate(6L)).thenReturn(Optional.of(booking));
        when(payments.save(payment)).thenReturn(payment);
        when(bookings.save(booking)).thenReturn(booking);
        assertEquals(PaymentStatus.FAILED, service.processSimulatedPayment(9L, false).getStatus());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test void rejectsSecondPendingPayment() {
        when(bookings.findByIdForUpdate(6L)).thenReturn(Optional.of(booking()));
        when(bookingFlights.countByBooking_Id(6L)).thenReturn(1L);
        when(passengers.countByBooking_Id(6L)).thenReturn(1L);
        when(payments.existsByBooking_IdAndStatus(6L, PaymentStatus.PENDING)).thenReturn(true);
        assertThrows(InvalidOperationException.class, () -> service.createPayment(6L, PaymentMethod.PAYPAL));
        verify(payments, never()).save(any());
    }

    @Test void requiresPassengerAndFlightBeforePayment() {
        when(bookings.findByIdForUpdate(6L)).thenReturn(Optional.of(booking()));
        when(bookingFlights.countByBooking_Id(6L)).thenReturn(1L);
        when(passengers.countByBooking_Id(6L)).thenReturn(0L);
        assertThrows(com.flightplatform.backend.exception.BusinessRuleException.class,
                () -> service.createPayment(6L, PaymentMethod.CARD));
        verify(payments, never()).save(any());
    }

    private static Booking booking() {
        Booking booking = new Booking(); booking.setId(6L); booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(new BigDecimal("88.25")); booking.setCurrency("EUR"); return booking;
    }
}
