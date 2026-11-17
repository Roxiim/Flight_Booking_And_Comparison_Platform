package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.repository.AircraftRepository;
import com.flightplatform.backend.repository.AircraftSeatRepository;
import com.flightplatform.backend.repository.AirlineRepository;
import com.flightplatform.backend.repository.AirportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FlightSearchIntegrationTest {
    @Autowired AirlineRepository airlineRepository;
    @Autowired AircraftRepository aircraftRepository;
    @Autowired AircraftSeatRepository aircraftSeatRepository;
    @Autowired AirportRepository airportRepository;
    @Autowired FlightService flightService;
    @Autowired FlightFareService fareService;

    @Test void filtersByRouteDateFareAndCabinWithoutDuplicateFlights() {
        Airline airline = new Airline(); airline.setName("Integration Air"); airline.setCode("IA"); airline.setCountry("RO");
        airline = airlineRepository.save(airline);
        Aircraft aircraft = new Aircraft(); aircraft.setAirline(airline); aircraft.setModel("A320");
        aircraft.setRegistrationNumber("YR-INT-01"); aircraft.setTotalSeats(2);
        aircraft = aircraftRepository.save(aircraft);
        AircraftSeat seat = new AircraftSeat(); seat.setAircraft(aircraft); seat.setSeatNumber("1A"); seat.setCabinClass(CabinClass.ECONOMY);
        aircraftSeatRepository.save(seat);
        Airport origin = airport("Origin Airport", "AAA");
        Airport destination = airport("Destination Airport", "BBB");
        LocalDate date = LocalDate.now().plusDays(3);
        Flight flight = new Flight(); flight.setAirline(airline); flight.setAircraft(aircraft); flight.setOriginAirport(origin);
        flight.setDestinationAirport(destination); flight.setFlightNumber("IA100"); flight.setDepartureTime(date.atTime(8, 0));
        flight.setArrivalTime(date.atTime(10, 0)); flight.setStatus(FlightStatus.SCHEDULED);
        flight = flightService.create(flight);
        fareService.create(flight.getId(), fare("Saver", "55.00"));
        fareService.create(flight.getId(), fare("Flex", "75.00"));

        Page<Flight> results = flightService.searchFlights("AAA", "BBB", date, airline.getId(),
                date.atTime(7, 0), date.atTime(9, 0), date.atTime(9, 0), date.atTime(11, 0),
                new BigDecimal("50.00"), new BigDecimal("80.00"), 0, CabinClass.ECONOMY, PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals("IA100", results.getContent().getFirst().getFlightNumber());
    }

    private Airport airport(String name, String code) {
        Airport airport = new Airport(); airport.setName(name); airport.setIataCode(code); airport.setCity("Test City"); airport.setCountry("Romania");
        return airportRepository.save(airport);
    }

    private static FlightFare fare(String name, String price) {
        FlightFare fare = new FlightFare(); fare.setFareName(name); fare.setPrice(new BigDecimal(price)); fare.setCurrency("EUR"); return fare;
    }
}
