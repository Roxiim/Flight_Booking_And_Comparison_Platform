package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Airport;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AirportService {
    private final AirportRepository airportRepository;

    public List<Airport> getAll() { return airportRepository.findAll(); }

    public Airport getByIataCode(String code) {
        String normalized = AirlineService.requireText(code, "IATA code").toUpperCase(Locale.ROOT);
        return airportRepository.findByIataCodeIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Airport not found with IATA code: " + normalized));
    }

    public List<Airport> search(String query) {
        String term = AirlineService.requireText(query, "Search query");
        return airportRepository.findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrCountryContainingIgnoreCase(term, term, term);
    }

    @Transactional
    public Airport create(Airport airport) {
        validate(airport);
        airport.setId(null);
        airport.setIataCode(airport.getIataCode().trim().toUpperCase(Locale.ROOT));
        ensureCodeAvailable(airport.getIataCode(), null);
        return airportRepository.save(airport);
    }

    @Transactional
    public Airport update(Long id, Airport changes) {
        validate(changes);
        Airport existing = airportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Airport", id));
        String code = changes.getIataCode().trim().toUpperCase(Locale.ROOT);
        ensureCodeAvailable(code, id);
        existing.setName(changes.getName().trim());
        existing.setIataCode(code);
        existing.setCity(changes.getCity().trim());
        existing.setCountry(changes.getCountry().trim());
        existing.setTimezone(AirlineService.trimToNull(changes.getTimezone()));
        return airportRepository.save(existing);
    }

    private void ensureCodeAvailable(String code, Long currentId) {
        airportRepository.findByIataCodeIgnoreCase(code).ifPresent(found -> {
            if (!found.getId().equals(currentId)) throw new DuplicateResourceException("IATA code already exists: " + code);
        });
    }

    private static void validate(Airport airport) {
        if (airport == null) throw new com.flightplatform.backend.exception.BusinessRuleException("Airport is required");
        AirlineService.requireText(airport.getName(), "Airport name");
        String code = AirlineService.requireText(airport.getIataCode(), "IATA code");
        if (!code.matches("(?i)^[a-z]{3}$")) throw new com.flightplatform.backend.exception.BusinessRuleException("IATA code must contain exactly three letters");
        AirlineService.requireText(airport.getCity(), "City");
        AirlineService.requireText(airport.getCountry(), "Country");
    }
}
