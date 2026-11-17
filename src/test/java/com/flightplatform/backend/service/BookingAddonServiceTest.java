package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.exception.InvalidOperationException;
import com.flightplatform.backend.repository.*;
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
class BookingAddonServiceTest {
    @Mock BookingAddonRepository bookingAddons;
    @Mock BookingFlightRepository bookingFlights;
    @Mock BookingRepository bookings;
    @Mock PassengerRepository passengers;
    @Mock AddonRepository addons;
    @Mock BookingService bookingService;
    @InjectMocks BookingAddonService service;

    @Test void addsAddonAtCurrentPriceAndRecalculatesTotal() {
        Booking booking = new Booking(); booking.setId(3L); booking.setStatus(BookingStatus.PENDING); booking.setCurrency("EUR");
        BookingFlight bookingFlight = new BookingFlight(); bookingFlight.setId(7L); bookingFlight.setBooking(booking);
        Addon addon = new Addon(); addon.setId(5L); addon.setPrice(new BigDecimal("4.50")); addon.setCurrency("EUR");
        when(bookingFlights.findById(7L)).thenReturn(Optional.of(bookingFlight));
        when(bookings.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));
        when(addons.findById(5L)).thenReturn(Optional.of(addon));
        when(bookingAddons.findByBookingFlight_IdAndAddon_IdAndPassengerIsNull(7L, 5L)).thenReturn(Optional.empty());
        when(bookingAddons.save(any(BookingAddon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BookingAddon line = service.add(7L, 5L, null, 2);
        assertEquals(2, line.getQuantity());
        assertEquals(new BigDecimal("4.50"), line.getPriceAtBooking());
        verify(bookingService).recalculateTotal(3L);
    }

    @Test void rejectsAddingAddonToCancelledBooking() {
        Booking booking = new Booking(); booking.setId(3L); booking.setStatus(BookingStatus.CANCELLED);
        BookingFlight bookingFlight = new BookingFlight(); bookingFlight.setId(7L); bookingFlight.setBooking(booking);
        when(bookingFlights.findById(7L)).thenReturn(Optional.of(bookingFlight));
        when(bookings.findByIdForUpdate(3L)).thenReturn(Optional.of(booking));
        assertThrows(InvalidOperationException.class, () -> service.add(7L, 5L, null, 1));
        verifyNoInteractions(addons);
    }
}
