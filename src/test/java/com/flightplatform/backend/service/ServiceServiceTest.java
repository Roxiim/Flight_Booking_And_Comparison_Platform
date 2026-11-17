package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Service;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.repository.FlightServiceRepository;
import com.flightplatform.backend.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {
    @Mock ServiceRepository services;
    @Mock FlightServiceRepository associations;
    @InjectMocks ServiceService service;

    @Test void rejectsDuplicateServiceName() {
        Service existing = new Service(); existing.setId(1L); existing.setName("Wifi");
        when(services.findByNameIgnoreCase("Wifi")).thenReturn(Optional.of(existing));
        Service duplicate = new Service(); duplicate.setName("Wifi");
        assertThrows(DuplicateResourceException.class, () -> service.create(duplicate));
        verify(services, never()).save(any());
    }
}
