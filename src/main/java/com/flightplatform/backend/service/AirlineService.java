package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Airline;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.AircraftRepository;
import com.flightplatform.backend.repository.AirlineRepository;
import com.flightplatform.backend.repository.FlightRepository;
import com.flightplatform.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AirlineService {
    private final AirlineRepository airlineRepository;
    private final UserRepository userRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;

    public Airline getById(Long id) {
        return airlineRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Airline", id));
    }

    public List<Airline> getAll() { return airlineRepository.findAll(); }

    public List<Airline> search(String query) {
        String term = requireText(query, "Search query");
        return airlineRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(term, term);
    }

    public Airline getByCode(String code) {
        String normalized = requireText(code, "Airline code").toUpperCase();
        return airlineRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found with code: " + normalized));
    }

    @Transactional
    public Airline create(Airline airline) {
        validate(airline);
        airline.setId(null);
        airline.setCode(airline.getCode().trim().toUpperCase());
        ensureCodeAvailable(airline.getCode(), null);
        return airlineRepository.save(airline);
    }

    @Transactional
    public Airline update(Long id, Airline changes) {
        validate(changes);
        Airline existing = getById(id);
        String newCode = changes.getCode().trim().toUpperCase();
        ensureCodeAvailable(newCode, id);
        existing.setName(changes.getName().trim());
        existing.setCode(newCode);
        existing.setCountry(changes.getCountry().trim());
        existing.setWebsite(trimToNull(changes.getWebsite()));
        existing.setSupportEmail(trimToNull(changes.getSupportEmail()));
        return airlineRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Airline airline = getById(id);
        if (userRepository.existsByAirline_Id(id) || aircraftRepository.existsByAirline_Id(id)
                || flightRepository.existsByAirline_Id(id)) {
            throw new BusinessRuleException("Airline cannot be deleted while users, aircraft or flights reference it");
        }
        airlineRepository.delete(airline);
    }

    private void ensureCodeAvailable(String code, Long currentId) {
        airlineRepository.findByCodeIgnoreCase(code).ifPresent(found -> {
            if (!found.getId().equals(currentId)) throw new DuplicateResourceException("Airline code already exists: " + code);
        });
    }

    private static void validate(Airline airline) {
        if (airline == null) throw new BusinessRuleException("Airline is required");
        requireText(airline.getName(), "Airline name");
        requireText(airline.getCode(), "Airline code");
        requireText(airline.getCountry(), "Airline country");
    }

    static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new BusinessRuleException(field + " is required");
        return value.trim();
    }

    static String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
