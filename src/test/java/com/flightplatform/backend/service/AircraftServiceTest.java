package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Aircraft;
import com.flightplatform.backend.entity.Airline;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.repository.AircraftRepository;
import com.flightplatform.backend.repository.AircraftSeatRepository;
import com.flightplatform.backend.repository.AirlineRepository;
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
class AircraftServiceTest {
    @Mock AircraftRepository aircraftRepository;
    @Mock AirlineRepository airlineRepository;
    @Mock AircraftSeatRepository seatRepository;
    @Mock FlightRepository flightRepository;
    @InjectMocks AircraftService service;

    @Test void createsAircraftForManagedAirline() {
        Airline airline = new Airline(); airline.setId(3L);
        Aircraft aircraft = aircraft(airline, 10);
        when(airlineRepository.findById(3L)).thenReturn(Optional.of(airline));
        when(aircraftRepository.findByRegistrationNumber("YR-ABC")).thenReturn(Optional.empty());
        when(aircraftRepository.save(aircraft)).thenReturn(aircraft);
        assertSame(aircraft, service.create(aircraft));
        assertEquals(airline, aircraft.getAirline());
    }

    @Test void refusesCapacityBelowConfiguredSeats() {
        Airline airline = new Airline(); airline.setId(3L);
        Aircraft existing = aircraft(airline, 10); existing.setId(6L);
        Aircraft changes = aircraft(airline, 4);
        when(aircraftRepository.findById(6L)).thenReturn(Optional.of(existing));
        when(aircraftRepository.findByRegistrationNumber("YR-ABC")).thenReturn(Optional.of(existing));
        when(seatRepository.countByAircraft_Id(6L)).thenReturn(5L);
        assertThrows(BusinessRuleException.class, () -> service.update(6L, changes));
        verify(aircraftRepository, never()).save(any());
    }

    private static Aircraft aircraft(Airline airline, int seats) {
        Aircraft aircraft = new Aircraft(); aircraft.setAirline(airline); aircraft.setModel("A320");
        aircraft.setRegistrationNumber("YR-ABC"); aircraft.setTotalSeats(seats); return aircraft;
    }
}
