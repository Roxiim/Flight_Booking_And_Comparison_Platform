package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Flight;
import com.flightplatform.backend.entity.FlightService;
import com.flightplatform.backend.entity.FlightServiceId;
import com.flightplatform.backend.entity.Service;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.FlightRepository;
import com.flightplatform.backend.repository.FlightServiceRepository;
import com.flightplatform.backend.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@org.springframework.stereotype.Service("flightServiceAssociationService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightServiceAssociationService {
    private final FlightRepository flightRepository;
    private final ServiceRepository serviceRepository;
    private final FlightServiceRepository associationRepository;

    public List<Service> getServicesForFlight(Long flightId) {
        if (!flightRepository.existsById(flightId)) throw new ResourceNotFoundException("Flight", flightId);
        return associationRepository.findByFlight_Id(flightId).stream().map(FlightService::getService).toList();
    }

    @Transactional
    public FlightService associate(Long flightId, Long serviceId) {
        Flight flight = flightRepository.findById(flightId).orElseThrow(() -> new ResourceNotFoundException("Flight", flightId));
        Service service = serviceRepository.findById(serviceId).orElseThrow(() -> new ResourceNotFoundException("Service", serviceId));
        if (associationRepository.existsByFlight_IdAndService_Id(flightId, serviceId)) {
            throw new DuplicateResourceException("Service is already associated with this flight");
        }
        FlightService association = new FlightService();
        association.setId(new FlightServiceId(flightId, serviceId));
        association.setFlight(flight);
        association.setService(service);
        return associationRepository.save(association);
    }

    @Transactional
    public void remove(Long flightId, Long serviceId) {
        FlightServiceId id = new FlightServiceId(flightId, serviceId);
        FlightService association = associationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight-service association not found"));
        associationRepository.delete(association);
    }
}
