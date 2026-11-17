package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Airport;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.repository.AirportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AirportServiceTest {
    @Mock AirportRepository airports;
    @InjectMocks AirportService service;

    @Test void createsAirportWithUppercaseIataCode() {
        Airport input = airport("otp");
        when(airports.findByIataCodeIgnoreCase("OTP")).thenReturn(Optional.empty());
        when(airports.save(input)).thenReturn(input);
        assertSame(input, service.create(input));
        assertEquals("OTP", input.getIataCode());
    }

    @Test void rejectsDuplicateIataCode() {
        Airport existing = airport("OTP"); existing.setId(9L);
        when(airports.findByIataCodeIgnoreCase("OTP")).thenReturn(Optional.of(existing));
        assertThrows(DuplicateResourceException.class, () -> service.create(airport("otp")));
        verify(airports, never()).save(any());
    }

    private static Airport airport(String code) {
        Airport airport = new Airport(); airport.setName("Henri Coanda"); airport.setIataCode(code);
        airport.setCity("Bucharest"); airport.setCountry("Romania"); return airport;
    }
}
