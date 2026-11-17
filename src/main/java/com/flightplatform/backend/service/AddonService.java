package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Addon;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.AddonRepository;
import com.flightplatform.backend.repository.BookingAddonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddonService {
    private final AddonRepository addonRepository;
    private final BookingAddonRepository bookingAddonRepository;

    public Addon getById(Long id) { return addonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Add-on", id)); }
    public List<Addon> getAvailableAddons() { return addonRepository.findAll(); }

    @Transactional
    public Addon create(Addon addon) {
        validate(addon);
        addon.setId(null);
        normalize(addon);
        return addonRepository.save(addon);
    }

    @Transactional
    public Addon update(Long id, Addon changes) {
        validate(changes);
        Addon existing = getById(id);
        existing.setName(changes.getName().trim());
        existing.setDescription(AirlineService.trimToNull(changes.getDescription()));
        existing.setPrice(changes.getPrice());
        existing.setCurrency(normalizeCurrency(changes.getCurrency()));
        return addonRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Addon addon = getById(id);
        if (bookingAddonRepository.existsByAddon_Id(id)) throw new BusinessRuleException("Add-on cannot be deleted after it has been added to a booking");
        addonRepository.delete(addon);
    }

    private static void validate(Addon addon) {
        if (addon == null) throw new BusinessRuleException("Add-on is required");
        AirlineService.requireText(addon.getName(), "Add-on name");
        if (addon.getPrice() == null || addon.getPrice().signum() < 0) throw new BusinessRuleException("Add-on price must be zero or greater");
        if (addon.getCurrency() != null && addon.getCurrency().length() != 3) throw new BusinessRuleException("Currency must contain three characters");
    }
    private static void normalize(Addon addon) {
        addon.setName(addon.getName().trim());
        addon.setDescription(AirlineService.trimToNull(addon.getDescription()));
        addon.setCurrency(normalizeCurrency(addon.getCurrency()));
    }
    private static String normalizeCurrency(String value) { return value == null || value.isBlank() ? "EUR" : value.trim().toUpperCase(); }
}
