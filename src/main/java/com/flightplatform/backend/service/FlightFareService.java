package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Flight;
import com.flightplatform.backend.entity.FlightFare;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.BookingFlightRepository;
import com.flightplatform.backend.repository.FlightFareRepository;
import com.flightplatform.backend.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightFareService {
    private final FlightFareRepository fareRepository;
    private final FlightRepository flightRepository;
    private final BookingFlightRepository bookingFlightRepository;

    public FlightFare getById(Long id) { return fareRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Flight fare", id)); }
    public List<FlightFare> getByFlight(Long flightId) {
        if (!flightRepository.existsById(flightId)) throw new ResourceNotFoundException("Flight", flightId);
        return fareRepository.findByFlight_Id(flightId);
    }
    public List<FlightFare> getAvailableFareOptions(Long flightId) { return getByFlight(flightId); }

    @Transactional
    public FlightFare create(Long flightId, FlightFare fare) {
        validate(fare);
        Flight flight = flightRepository.findById(flightId).orElseThrow(() -> new ResourceNotFoundException("Flight", flightId));
        fare.setId(null);
        fare.setFlight(flight);
        normalize(fare);
        return fareRepository.save(fare);
    }

    @Transactional
    public FlightFare update(Long id, FlightFare changes) {
        validate(changes);
        FlightFare existing = getById(id);
        if (bookingFlightRepository.existsByFare_Id(id)) {
            throw new BusinessRuleException("Fare terms cannot be changed after the fare has been used by a booking");
        }
        existing.setFareName(changes.getFareName().trim());
        existing.setPrice(changes.getPrice());
        existing.setCurrency(normalizeCurrency(changes.getCurrency()));
        existing.setPersonalItemIncluded(changes.getPersonalItemIncluded());
        existing.setCabinBagIncluded(changes.getCabinBagIncluded());
        existing.setCheckedBagsIncluded(changes.getCheckedBagsIncluded());
        existing.setCheckedBagWeightKg(changes.getCheckedBagWeightKg());
        existing.setChangesAllowed(changes.getChangesAllowed());
        existing.setRefundAllowed(changes.getRefundAllowed());
        return fareRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        FlightFare fare = getById(id);
        if (bookingFlightRepository.existsByFare_Id(id)) throw new BusinessRuleException("Fare cannot be deleted after it has been used by a booking");
        fareRepository.delete(fare);
    }

    private static void validate(FlightFare fare) {
        if (fare == null) throw new BusinessRuleException("Fare is required");
        AirlineService.requireText(fare.getFareName(), "Fare name");
        if (fare.getPrice() == null || fare.getPrice().signum() < 0) throw new BusinessRuleException("Fare price must be zero or greater");
        if (fare.getCurrency() != null && fare.getCurrency().length() != 3) throw new BusinessRuleException("Currency must contain three characters");
        if (fare.getCheckedBagsIncluded() != null && fare.getCheckedBagsIncluded() < 0) throw new BusinessRuleException("Included checked bags cannot be negative");
        BigDecimal weight = fare.getCheckedBagWeightKg();
        if (weight != null && weight.signum() < 0) throw new BusinessRuleException("Checked bag weight cannot be negative");
    }

    private static void normalize(FlightFare fare) {
        fare.setFareName(fare.getFareName().trim());
        fare.setCurrency(normalizeCurrency(fare.getCurrency()));
    }

    private static String normalizeCurrency(String value) { return value == null || value.isBlank() ? "EUR" : value.trim().toUpperCase(); }
}
