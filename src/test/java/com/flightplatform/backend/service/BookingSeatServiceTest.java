package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.exception.DuplicateResourceException;
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
class BookingSeatServiceTest {
    @Mock BookingSeatRepository bookingSeats;
    @Mock BookingFlightRepository bookingFlights;
    @Mock BookingRepository bookings;
    @Mock PassengerRepository passengers;
    @Mock AircraftSeatRepository aircraftSeats;
    @Mock FlightRepository flights;
    @Mock BookingService bookingService;
    @InjectMocks BookingSeatService service;

    @Test void reservesSeatAndRecalculatesBookingTotal() {
        Fixture f = fixture();
        when(bookingFlights.findById(11L)).thenReturn(Optional.of(f.bookingFlight));
        when(flights.findByIdForUpdate(3L)).thenReturn(Optional.of(f.flight));
        when(bookings.findByIdForUpdate(5L)).thenReturn(Optional.of(f.booking));
        when(passengers.findById(8L)).thenReturn(Optional.of(f.passenger));
        when(aircraftSeats.findById(9L)).thenReturn(Optional.of(f.seat));
        when(bookingSeats.existsByFlight_IdAndAircraftSeat_Id(3L, 9L)).thenReturn(false);
        when(bookingSeats.existsByBookingFlight_IdAndPassenger_Id(11L, 8L)).thenReturn(false);
        when(bookingSeats.saveAndFlush(any(BookingSeat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BookingSeat result = service.reserveSeat(11L, 8L, 9L, new BigDecimal("12.00"));
        assertSame(f.flight, result.getFlight());
        assertEquals(new BigDecimal("12.00"), result.getSeatPrice());
        verify(bookingService).recalculateTotal(5L);
    }

    @Test void preventsDoubleBookingForFlightSeat() {
        Fixture f = fixture();
        when(bookingFlights.findById(11L)).thenReturn(Optional.of(f.bookingFlight));
        when(flights.findByIdForUpdate(3L)).thenReturn(Optional.of(f.flight));
        when(bookings.findByIdForUpdate(5L)).thenReturn(Optional.of(f.booking));
        when(passengers.findById(8L)).thenReturn(Optional.of(f.passenger));
        when(aircraftSeats.findById(9L)).thenReturn(Optional.of(f.seat));
        when(bookingSeats.existsByFlight_IdAndAircraftSeat_Id(3L, 9L)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.reserveSeat(11L, 8L, 9L, BigDecimal.ZERO));
        verify(bookingSeats, never()).saveAndFlush(any());
    }

    private static Fixture fixture() {
        Airline airline = new Airline(); airline.setId(1L);
        Aircraft aircraft = new Aircraft(); aircraft.setId(2L); aircraft.setAirline(airline);
        Flight flight = new Flight(); flight.setId(3L); flight.setAircraft(aircraft); flight.setStatus(FlightStatus.SCHEDULED);
        Booking booking = new Booking(); booking.setId(5L); booking.setStatus(BookingStatus.PENDING);
        BookingFlight bookingFlight = new BookingFlight(); bookingFlight.setId(11L); bookingFlight.setBooking(booking); bookingFlight.setFlight(flight);
        Passenger passenger = new Passenger(); passenger.setId(8L); passenger.setBooking(booking);
        AircraftSeat seat = new AircraftSeat(); seat.setId(9L); seat.setAircraft(aircraft);
        return new Fixture(flight, booking, bookingFlight, passenger, seat);
    }
    private record Fixture(Flight flight, Booking booking, BookingFlight bookingFlight, Passenger passenger, AircraftSeat seat) { }
}
