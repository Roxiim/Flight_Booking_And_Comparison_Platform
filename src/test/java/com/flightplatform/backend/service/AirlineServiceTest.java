package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Airline;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.repository.AircraftRepository;
import com.flightplatform.backend.repository.AirlineRepository;
import com.flightplatform.backend.repository.FlightRepository;
import com.flightplatform.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirlineServiceTest {
    @Mock AirlineRepository airlines;
    @Mock UserRepository users;
    @Mock AircraftRepository aircraft;
    @Mock FlightRepository flights;
    @InjectMocks AirlineService service;

    @Test void createsAirlineWithNormalizedCode() {
        Airline input = airline("North Air", "na");
        when(airlines.findByCodeIgnoreCase("NA")).thenReturn(Optional.empty());
        when(airlines.save(input)).thenReturn(input);
        assertSame(input, service.create(input));
        assertEquals("NA", input.getCode());
        verify(airlines).save(input);
    }

    @Test void rejectsDuplicateCode() {
        Airline existing = airline("Existing", "NA"); existing.setId(4L);
        when(airlines.findByCodeIgnoreCase("NA")).thenReturn(Optional.of(existing));
        assertThrows(DuplicateResourceException.class, () -> service.create(airline("New", "NA")));
        verify(airlines, never()).save(any());
    }

    @Test void refusesDeleteWhenAircraftReferencesAirline() {
        Airline existing = airline("North Air", "NA"); existing.setId(4L);
        when(airlines.findById(4L)).thenReturn(Optional.of(existing));
        when(users.existsByAirline_Id(4L)).thenReturn(false);
        when(aircraft.existsByAirline_Id(4L)).thenReturn(true);
        assertThrows(BusinessRuleException.class, () -> service.delete(4L));
        verify(airlines, never()).delete(any(Airline.class));
    }

    private static Airline airline(String name, String code) {
        Airline airline = new Airline(); airline.setName(name); airline.setCode(code); airline.setCountry("RO"); return airline;
    }
}
