package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Addon;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.repository.AddonRepository;
import com.flightplatform.backend.repository.BookingAddonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddonServiceTest {
    @Mock AddonRepository addons;
    @Mock BookingAddonRepository bookingAddons;
    @InjectMocks AddonService service;

    @Test void rejectsNegativeAddonPrice() {
        Addon addon = new Addon(); addon.setName("Bag"); addon.setPrice(new BigDecimal("-1"));
        assertThrows(BusinessRuleException.class, () -> service.create(addon));
        verifyNoInteractions(addons);
    }
}
