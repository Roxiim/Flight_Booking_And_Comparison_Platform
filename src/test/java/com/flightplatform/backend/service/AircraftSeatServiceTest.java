package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Aircraft;
import com.flightplatform.backend.entity.AircraftSeat;
import com.flightplatform.backend.entity.CabinClass;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.repository.AircraftRepository;
import com.flightplatform.backend.repository.AircraftSeatRepository;
import com.flightplatform.backend.repository.BookingSeatRepository;
import com.flightplatform.backend.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AircraftSeatServiceTest {
    @Mock AircraftSeatRepository seatRepository;
    @Mock AircraftRepository aircraftRepository;
    @Mock FlightRepository flightRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @InjectMocks AircraftSeatService service;

    @Test void addsSeatWithinAircraftCapacity() {
        Aircraft aircraft = new Aircraft(); aircraft.setId(2L); aircraft.setTotalSeats(100);
        AircraftSeat seat = new AircraftSeat(); seat.setSeatNumber("1a"); seat.setCabinClass(CabinClass.ECONOMY);
        when(aircraftRepository.findById(2L)).thenReturn(Optional.of(aircraft));
        when(seatRepository.existsByAircraft_IdAndSeatNumberIgnoreCase(2L, "1A")).thenReturn(false);
        when(seatRepository.countByAircraft_Id(2L)).thenReturn(10L);
        when(seatRepository.save(seat)).thenReturn(seat);
        assertSame(seat, service.addSeat(2L, seat));
        assertEquals("1A", seat.getSeatNumber());
        assertSame(aircraft, seat.getAircraft());
    }

    @Test void rejectsSeatWhenCapacityIsFull() {
        Aircraft aircraft = new Aircraft(); aircraft.setId(2L); aircraft.setTotalSeats(1);
        AircraftSeat seat = new AircraftSeat(); seat.setSeatNumber("1A"); seat.setCabinClass(CabinClass.BUSINESS);
        when(aircraftRepository.findById(2L)).thenReturn(Optional.of(aircraft));
        when(seatRepository.existsByAircraft_IdAndSeatNumberIgnoreCase(2L, "1A")).thenReturn(false);
        when(seatRepository.countByAircraft_Id(2L)).thenReturn(1L);
        assertThrows(BusinessRuleException.class, () -> service.addSeat(2L, seat));
        verify(seatRepository, never()).save(any());
    }
}
