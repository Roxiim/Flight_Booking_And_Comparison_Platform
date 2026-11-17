package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Flight;
import com.flightplatform.backend.entity.Service;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.repository.FlightRepository;
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
class FlightServiceAssociationServiceTest {
    @Mock FlightRepository flights;
    @Mock ServiceRepository services;
    @Mock FlightServiceRepository associations;
    @InjectMocks FlightServiceAssociationService service;

    @Test void rejectsDuplicateFlightServiceAssociation() {
        when(flights.findById(1L)).thenReturn(Optional.of(new Flight()));
        when(services.findById(2L)).thenReturn(Optional.of(new Service()));
        when(associations.existsByFlight_IdAndService_Id(1L, 2L)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.associate(1L, 2L));
        verify(associations, never()).save(any());
    }
}
