package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightService {
    private final FlightRepository flightRepository;
    private final AirlineRepository airlineRepository;
    private final AircraftRepository aircraftRepository;
    private final AirportRepository airportRepository;
    private final FlightFareRepository fareRepository;
    private final FlightServiceRepository flightServiceRepository;
    private final BookingFlightRepository bookingFlightRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final AircraftSeatRepository aircraftSeatRepository;

    public Flight getById(Long id) {
        Flight flight = flightRepository.findWithDetailsById(id).orElseThrow(() -> new ResourceNotFoundException("Flight", id));
        flight.getFares().forEach(FlightFare::getId);
        flight.getFlightServices().forEach(association -> association.getService().getName());
        return flight;
    }

    public Page<Flight> searchFlights(String originIata, String destinationIata, LocalDate departureDate,
            Long airlineId, LocalDateTime departureFrom, LocalDateTime departureTo,
            LocalDateTime arrivalFrom, LocalDateTime arrivalTo, BigDecimal minPrice, BigDecimal maxPrice,
            Integer numberOfStops, CabinClass cabinClass, Pageable pageable) {
        if (originIata == null || originIata.isBlank() || destinationIata == null || destinationIata.isBlank() || departureDate == null) {
            throw new BusinessRuleException("Origin, destination and departure date are required");
        }
        if (originIata.equalsIgnoreCase(destinationIata)) throw new BusinessRuleException("Origin and destination must differ");
        if (pageable == null) throw new BusinessRuleException("Page request is required");
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) throw new BusinessRuleException("Price filters cannot be negative");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) throw new BusinessRuleException("Minimum price cannot exceed maximum price");
        if (numberOfStops != null && numberOfStops < 0) throw new BusinessRuleException("Number of stops cannot be negative");
        if (departureFrom != null && departureTo != null && departureFrom.isAfter(departureTo)) throw new BusinessRuleException("Departure range is invalid");
        if (arrivalFrom != null && arrivalTo != null && arrivalFrom.isAfter(arrivalTo)) throw new BusinessRuleException("Arrival range is invalid");

        Specification<Flight> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(cb.upper(root.get("originAirport").get("iataCode")), originIata.trim().toUpperCase()));
            predicates.add(cb.equal(cb.upper(root.get("destinationAirport").get("iataCode")), destinationIata.trim().toUpperCase()));
            LocalDateTime dayStart = departureDate.atStartOfDay();
            predicates.add(cb.greaterThanOrEqualTo(root.get("departureTime"), dayStart));
            predicates.add(cb.lessThan(root.get("departureTime"), dayStart.plusDays(1)));
            predicates.add(root.get("status").in(FlightStatus.SCHEDULED, FlightStatus.DELAYED));
            if (airlineId != null) predicates.add(cb.equal(root.get("airline").get("id"), airlineId));
            if (departureFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("departureTime"), departureFrom));
            if (departureTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("departureTime"), departureTo));
            if (arrivalFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("arrivalTime"), arrivalFrom));
            if (arrivalTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("arrivalTime"), arrivalTo));
            if (numberOfStops != null) predicates.add(cb.equal(root.get("numberOfStops"), numberOfStops));
            if (minPrice != null || maxPrice != null) {
                Join<Flight, FlightFare> fare = root.join("fares", JoinType.INNER);
                if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(fare.get("price"), minPrice));
                if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(fare.get("price"), maxPrice));
            }
            if (cabinClass != null) {
                Subquery<Long> seatMatch = query.subquery(Long.class);
                Root<AircraftSeat> seat = seatMatch.from(AircraftSeat.class);
                seatMatch.select(seat.get("id")).where(
                        cb.equal(seat.get("aircraft").get("id"), root.get("aircraft").get("id")),
                        cb.equal(seat.get("cabinClass"), cabinClass));
                predicates.add(cb.exists(seatMatch));
            }
            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return flightRepository.findAll(specification, pageable);
    }

    @Transactional
    public Flight create(Flight flight) {
        validate(flight);
        flight.setId(null);
        attachReferences(flight);
        if (flight.getNumberOfStops() == null) flight.setNumberOfStops(0);
        if (flight.getStatus() == null) flight.setStatus(FlightStatus.SCHEDULED);
        return flightRepository.save(flight);
    }

    @Transactional
    public Flight update(Long id, Flight changes) {
        validate(changes);
        Flight existing = flightRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Flight", id));
        attachReferences(changes);
        boolean routeChanged = !existing.getAircraft().getId().equals(changes.getAircraft().getId())
                || !existing.getOriginAirport().getId().equals(changes.getOriginAirport().getId())
                || !existing.getDestinationAirport().getId().equals(changes.getDestinationAirport().getId())
                || !existing.getDepartureTime().equals(changes.getDepartureTime())
                || !existing.getArrivalTime().equals(changes.getArrivalTime());
        if (routeChanged && bookingFlightRepository.existsByFlight_Id(id)) {
            throw new BusinessRuleException("Flight route, aircraft and times cannot change after bookings exist");
        }
        existing.setAirline(changes.getAirline());
        existing.setAircraft(changes.getAircraft());
        existing.setOriginAirport(changes.getOriginAirport());
        existing.setDestinationAirport(changes.getDestinationAirport());
        existing.setFlightNumber(changes.getFlightNumber().trim());
        existing.setDepartureTime(changes.getDepartureTime());
        existing.setArrivalTime(changes.getArrivalTime());
        existing.setNumberOfStops(changes.getNumberOfStops() == null ? 0 : changes.getNumberOfStops());
        existing.setStatus(changes.getStatus() == null ? existing.getStatus() : changes.getStatus());
        return flightRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Flight flight = flightRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Flight", id));
        if (bookingFlightRepository.existsByFlight_Id(id) || bookingSeatRepository.countByFlight_Id(id) > 0
                || fareRepository.existsByFlight_Id(id) || flightServiceRepository.existsByFlight_Id(id)) {
            throw new BusinessRuleException("Flight cannot be deleted while fares, services or bookings reference it");
        }
        flightRepository.delete(flight);
    }

    public boolean hasAvailability(Long flightId) {
        Flight flight = flightRepository.findById(flightId).orElseThrow(() -> new ResourceNotFoundException("Flight", flightId));
        if (flight.getStatus() != FlightStatus.SCHEDULED && flight.getStatus() != FlightStatus.DELAYED) return false;
        long totalSeats = aircraftSeatRepository.countByAircraft_Id(flight.getAircraft().getId());
        return totalSeats > bookingSeatRepository.countByFlight_Id(flightId);
    }

    private void attachReferences(Flight flight) {
        flight.setAirline(airlineRepository.findById(flight.getAirline().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Airline", flight.getAirline().getId())));
        flight.setAircraft(aircraftRepository.findById(flight.getAircraft().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft", flight.getAircraft().getId())));
        flight.setOriginAirport(airportRepository.findById(flight.getOriginAirport().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Airport", flight.getOriginAirport().getId())));
        flight.setDestinationAirport(airportRepository.findById(flight.getDestinationAirport().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Airport", flight.getDestinationAirport().getId())));
        if (!flight.getAircraft().getAirline().getId().equals(flight.getAirline().getId())) {
            throw new BusinessRuleException("Aircraft must belong to the same airline as the flight");
        }
    }

    private static void validate(Flight flight) {
        if (flight == null) throw new BusinessRuleException("Flight is required");
        if (flight.getAirline() == null || flight.getAircraft() == null || flight.getOriginAirport() == null || flight.getDestinationAirport() == null) {
            throw new BusinessRuleException("Airline, aircraft, origin and destination are required");
        }
        AirlineService.requireText(flight.getFlightNumber(), "Flight number");
        if (flight.getDepartureTime() == null || flight.getArrivalTime() == null) throw new BusinessRuleException("Departure and arrival times are required");
        if (!flight.getArrivalTime().isAfter(flight.getDepartureTime())) throw new BusinessRuleException("Arrival time must be after departure time");
        if (flight.getNumberOfStops() != null && flight.getNumberOfStops() < 0) throw new BusinessRuleException("Number of stops cannot be negative");
        if (flight.getAirline().getId() == null || flight.getAircraft().getId() == null || flight.getOriginAirport().getId() == null || flight.getDestinationAirport().getId() == null) {
            throw new BusinessRuleException("Flight references must refer to existing records");
        }
        if (flight.getOriginAirport().getId().equals(flight.getDestinationAirport().getId())) throw new BusinessRuleException("Origin and destination airports must differ");
    }
}
