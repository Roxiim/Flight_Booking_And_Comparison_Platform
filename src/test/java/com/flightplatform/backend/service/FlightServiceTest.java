package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {
    @Mock FlightRepository flightRepository;
    @Mock AirlineRepository airlineRepository;
    @Mock AircraftRepository aircraftRepository;
    @Mock AirportRepository airportRepository;
    @Mock FlightFareRepository fareRepository;
    @Mock FlightServiceRepository flightServiceRepository;
    @Mock BookingFlightRepository bookingFlightRepository;
    @Mock BookingSeatRepository bookingSeatRepository;
    @Mock AircraftSeatRepository aircraftSeatRepository;
    @InjectMocks com.flightplatform.backend.service.FlightService service;

    @Test void rejectsSameOriginAndDestinationSearch() {
        assertThrows(BusinessRuleException.class, () -> service.searchFlights("OTP", "otp", java.time.LocalDate.now(),
                null, null, null, null, null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10)));
        verifyNoInteractions(flightRepository);
    }

    @Test void rejectsArrivalNotAfterDeparture() {
        Flight flight = flight();
        flight.setArrivalTime(flight.getDepartureTime());
        assertThrows(BusinessRuleException.class, () -> service.create(flight));
        verifyNoInteractions(airlineRepository);
    }

    @Test void createsFlightWithResolvedReferencesAndDefaultStatus() {
        Flight flight = flight();
        Airline airline = flight.getAirline(); Aircraft aircraft = flight.getAircraft();
        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
        when(aircraftRepository.findById(2L)).thenReturn(Optional.of(aircraft));
        when(airportRepository.findById(3L)).thenReturn(Optional.of(flight.getOriginAirport()));
        when(airportRepository.findById(4L)).thenReturn(Optional.of(flight.getDestinationAirport()));
        when(flightRepository.save(flight)).thenReturn(flight);
        assertSame(flight, service.create(flight));
        assertEquals(FlightStatus.SCHEDULED, flight.getStatus());
        assertEquals(0, flight.getNumberOfStops());
    }

    private static Flight flight() {
        Airline airline = new Airline(); airline.setId(1L);
        Aircraft aircraft = new Aircraft(); aircraft.setId(2L); aircraft.setAirline(airline);
        Airport origin = new Airport(); origin.setId(3L);
        Airport destination = new Airport(); destination.setId(4L);
        Flight flight = new Flight(); flight.setAirline(airline); flight.setAircraft(aircraft);
        flight.setOriginAirport(origin); flight.setDestinationAirport(destination); flight.setFlightNumber("FP101");
        flight.setDepartureTime(LocalDateTime.of(2027, 1, 1, 10, 0)); flight.setArrivalTime(LocalDateTime.of(2027, 1, 1, 12, 0));
        return flight;
    }
}
