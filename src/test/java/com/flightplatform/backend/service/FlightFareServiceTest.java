package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.FlightFare;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.repository.BookingFlightRepository;
import com.flightplatform.backend.repository.FlightFareRepository;
import com.flightplatform.backend.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class FlightFareServiceTest {
    @Mock FlightFareRepository fares;
    @Mock FlightRepository flights;
    @Mock BookingFlightRepository bookings;
    @InjectMocks FlightFareService service;

    @Test void rejectsNegativeFarePrice() {
        FlightFare fare = new FlightFare(); fare.setFareName("Basic"); fare.setPrice(new BigDecimal("-1.00"));
        assertThrows(BusinessRuleException.class, () -> service.create(1L, fare));
        verifyNoInteractions(flights);
    }

    @Test void rejectsNegativeIncludedBags() {
        FlightFare fare = new FlightFare(); fare.setFareName("Basic"); fare.setPrice(BigDecimal.ZERO); fare.setCheckedBagsIncluded(-1);
        assertThrows(BusinessRuleException.class, () -> service.update(5L, fare));
        verify(fares, never()).save(any());
    }

    @Test void preventsChangingFareTermsAfterBookingUsesFare() {
        FlightFare stored = new FlightFare(); stored.setId(5L);
        FlightFare changes = new FlightFare(); changes.setFareName("Flexible"); changes.setPrice(BigDecimal.TEN);
        when(fares.findById(5L)).thenReturn(Optional.of(stored));
        when(bookings.existsByFare_Id(5L)).thenReturn(true);
        assertThrows(BusinessRuleException.class, () -> service.update(5L, changes));
        verify(fares, never()).save(any());
    }
}
