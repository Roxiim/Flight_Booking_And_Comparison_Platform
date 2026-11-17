package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Aircraft;
import com.flightplatform.backend.entity.Airline;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.AircraftRepository;
import com.flightplatform.backend.repository.AircraftSeatRepository;
import com.flightplatform.backend.repository.AirlineRepository;
import com.flightplatform.backend.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AircraftService {
    private final AircraftRepository aircraftRepository;
    private final AirlineRepository airlineRepository;
    private final AircraftSeatRepository aircraftSeatRepository;
    private final FlightRepository flightRepository;

    public Aircraft getById(Long id) { return aircraftRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Aircraft", id)); }
    public List<Aircraft> getAll() { return aircraftRepository.findAll(); }
    public List<Aircraft> getByAirline(Long airlineId) {
        if (!airlineRepository.existsById(airlineId)) throw new ResourceNotFoundException("Airline", airlineId);
        return aircraftRepository.findByAirline_Id(airlineId);
    }

    @Transactional
    public Aircraft create(Aircraft aircraft) {
        validate(aircraft);
        aircraft.setId(null);
        aircraft.setAirline(requireManagedAirline(aircraft.getAirline()));
        aircraft.setModel(aircraft.getModel().trim());
        aircraft.setRegistrationNumber(aircraft.getRegistrationNumber().trim().toUpperCase());
        if (aircraftRepository.findByRegistrationNumber(aircraft.getRegistrationNumber()).isPresent()) {
            throw new DuplicateResourceException("Aircraft registration number already exists");
        }
        return aircraftRepository.save(aircraft);
    }

    @Transactional
    public Aircraft update(Long id, Aircraft changes) {
        validate(changes);
        Aircraft existing = getById(id);
        String registration = changes.getRegistrationNumber().trim().toUpperCase();
        aircraftRepository.findByRegistrationNumber(registration).ifPresent(found -> {
            if (!found.getId().equals(id)) throw new DuplicateResourceException("Aircraft registration number already exists");
        });
        long configuredSeats = aircraftSeatRepository.countByAircraft_Id(id);
        if (changes.getTotalSeats() < configuredSeats) throw new BusinessRuleException("Total seats cannot be less than the configured seat count");
        existing.setAirline(requireManagedAirline(changes.getAirline()));
        existing.setModel(changes.getModel().trim());
        existing.setRegistrationNumber(registration);
        existing.setTotalSeats(changes.getTotalSeats());
        return aircraftRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Aircraft aircraft = getById(id);
        if (flightRepository.existsByAircraft_Id(id)) throw new BusinessRuleException("Aircraft cannot be deleted while flights reference it");
        if (aircraftSeatRepository.countByAircraft_Id(id) > 0) throw new BusinessRuleException("Remove aircraft seats before deleting the aircraft");
        aircraftRepository.delete(aircraft);
    }

    private Airline requireManagedAirline(Airline airline) {
        if (airline == null || airline.getId() == null) throw new BusinessRuleException("An existing airline is required");
        return airlineRepository.findById(airline.getId()).orElseThrow(() -> new ResourceNotFoundException("Airline", airline.getId()));
    }

    private static void validate(Aircraft aircraft) {
        if (aircraft == null) throw new BusinessRuleException("Aircraft is required");
        AirlineService.requireText(aircraft.getModel(), "Aircraft model");
        AirlineService.requireText(aircraft.getRegistrationNumber(), "Registration number");
        if (aircraft.getTotalSeats() == null || aircraft.getTotalSeats() <= 0) throw new BusinessRuleException("Total seats must be positive");
        if (aircraft.getAirline() == null) throw new BusinessRuleException("Airline is required");
    }
}
