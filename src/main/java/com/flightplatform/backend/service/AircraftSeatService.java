package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Aircraft;
import com.flightplatform.backend.entity.AircraftSeat;
import com.flightplatform.backend.entity.Flight;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.AircraftRepository;
import com.flightplatform.backend.repository.AircraftSeatRepository;
import com.flightplatform.backend.repository.BookingSeatRepository;
import com.flightplatform.backend.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AircraftSeatService {
    private final AircraftSeatRepository seatRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public List<AircraftSeat> getByAircraft(Long aircraftId) {
        if (!aircraftRepository.existsById(aircraftId)) throw new ResourceNotFoundException("Aircraft", aircraftId);
        return seatRepository.findByAircraft_Id(aircraftId);
    }

    public List<AircraftSeat> getAvailableForFlight(Long flightId) {
        Flight flight = flightRepository.findById(flightId).orElseThrow(() -> new ResourceNotFoundException("Flight", flightId));
        if (flight.getStatus() == null || flight.getStatus() == com.flightplatform.backend.entity.FlightStatus.CANCELLED
                || flight.getStatus() == com.flightplatform.backend.entity.FlightStatus.COMPLETED) {
            throw new BusinessRuleException("Seats are not available for this flight");
        }
        Set<Long> occupiedSeatIds = bookingSeatRepository.findByFlight_Id(flightId).stream()
                .map(bookingSeat -> bookingSeat.getAircraftSeat().getId()).collect(Collectors.toSet());
        return seatRepository.findByAircraft_Id(flight.getAircraft().getId()).stream()
                .filter(seat -> !occupiedSeatIds.contains(seat.getId()))
                .toList();
    }

    @Transactional
    public AircraftSeat addSeat(Long aircraftId, AircraftSeat seat) {
        if (seat == null) throw new BusinessRuleException("Aircraft seat is required");
        Aircraft aircraft = aircraftRepository.findById(aircraftId).orElseThrow(() -> new ResourceNotFoundException("Aircraft", aircraftId));
        String number = AirlineService.requireText(seat.getSeatNumber(), "Seat number").toUpperCase();
        if (seat.getCabinClass() == null) throw new BusinessRuleException("Cabin class is required");
        if (seatRepository.existsByAircraft_IdAndSeatNumberIgnoreCase(aircraftId, number)) throw new BusinessRuleException("Seat number already exists for this aircraft");
        if (seatRepository.countByAircraft_Id(aircraftId) >= aircraft.getTotalSeats()) throw new BusinessRuleException("Aircraft seat configuration exceeds total seat capacity");
        seat.setId(null);
        seat.setAircraft(aircraft);
        seat.setSeatNumber(number);
        return seatRepository.save(seat);
    }

    @Transactional
    public void removeSeat(Long seatId) {
        AircraftSeat seat = seatRepository.findById(seatId).orElseThrow(() -> new ResourceNotFoundException("Aircraft seat", seatId));
        if (bookingSeatRepository.existsByAircraftSeat_Id(seatId)) throw new BusinessRuleException("Seat cannot be removed while a booking references it");
        seatRepository.delete(seat);
    }
}
