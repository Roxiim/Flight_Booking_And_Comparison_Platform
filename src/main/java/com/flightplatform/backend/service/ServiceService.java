package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Service;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.FlightServiceRepository;
import com.flightplatform.backend.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@org.springframework.stereotype.Service("catalogService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceService {
    private final ServiceRepository serviceRepository;
    private final FlightServiceRepository flightServiceRepository;

    public Service getById(Long id) { return serviceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service", id)); }
    public List<Service> getAll() { return serviceRepository.findAll(); }
    public Service getByName(String name) {
        String normalized = AirlineService.requireText(name, "Service name");
        return serviceRepository.findByNameIgnoreCase(normalized).orElseThrow(() -> new ResourceNotFoundException("Service not found: " + normalized));
    }

    @Transactional
    public Service create(Service service) {
        validate(service);
        service.setId(null);
        service.setName(service.getName().trim());
        if (serviceRepository.findByNameIgnoreCase(service.getName()).isPresent()) throw new DuplicateResourceException("Service name already exists");
        return serviceRepository.save(service);
    }

    @Transactional
    public Service update(Long id, Service changes) {
        validate(changes);
        Service existing = getById(id);
        String name = changes.getName().trim();
        serviceRepository.findByNameIgnoreCase(name).ifPresent(found -> {
            if (!found.getId().equals(id)) throw new DuplicateResourceException("Service name already exists");
        });
        existing.setName(name);
        existing.setDescription(AirlineService.trimToNull(changes.getDescription()));
        return serviceRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Service service = getById(id);
        if (flightServiceRepository.existsByService_Id(id)) throw new BusinessRuleException("Service cannot be deleted while flights reference it");
        serviceRepository.delete(service);
    }

    private static void validate(Service service) {
        if (service == null) throw new BusinessRuleException("Service is required");
        AirlineService.requireText(service.getName(), "Service name");
    }
}
