package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Airline;
import com.flightplatform.backend.entity.User;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.repository.AirlineRepository;
import com.flightplatform.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private static final int PASSWORD_ITERATIONS = 310_000;
    private static final int PASSWORD_KEY_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final UserRepository userRepository;
    private final AirlineRepository airlineRepository;

    public User getById(Long id) { return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id)); }
    public List<User> getAll() { return userRepository.findAll(); }

    public User getByEmail(String email) {
        String normalized = normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalized));
    }

    @Transactional
    public User create(User user) {
        validate(user, true);
        user.setId(null);
        user.setEmail(normalizeEmail(user.getEmail()));
        ensureEmailAvailable(user.getEmail(), null);
        attachManagedAirline(user);
        user.setPassword(hashPassword(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, User changes) {
        validate(changes, false);
        User existing = getById(id);
        String email = normalizeEmail(changes.getEmail());
        ensureEmailAvailable(email, id);
        existing.setFirstName(changes.getFirstName().trim());
        existing.setLastName(changes.getLastName().trim());
        existing.setEmail(email);
        existing.setPhoneNumber(AirlineService.trimToNull(changes.getPhoneNumber()));
        existing.setRole(changes.getRole());
        if (changes.getAirline() == null) {
            existing.setAirline(null);
        } else {
            User airlineHolder = new User();
            airlineHolder.setAirline(changes.getAirline());
            attachManagedAirline(airlineHolder);
            existing.setAirline(airlineHolder.getAirline());
        }
        if (changes.getPassword() != null && !changes.getPassword().isBlank()) {
            existing.setPassword(hashPassword(changes.getPassword()));
        }
        return userRepository.save(existing);
    }

    private void attachManagedAirline(User user) {
        Airline airline = user.getAirline();
        if (airline == null) return;
        if (airline.getId() == null) throw new BusinessRuleException("An existing airline must be selected for this user");
        user.setAirline(airlineRepository.findById(airline.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Airline", airline.getId())));
    }

    private void ensureEmailAvailable(String email, Long currentId) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(found -> {
            if (!found.getId().equals(currentId)) throw new DuplicateResourceException("Email is already registered");
        });
    }

    private static void validate(User user, boolean passwordRequired) {
        if (user == null) throw new BusinessRuleException("User is required");
        AirlineService.requireText(user.getFirstName(), "First name");
        AirlineService.requireText(user.getLastName(), "Last name");
        String email = normalizeEmail(user.getEmail());
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new BusinessRuleException("A valid email address is required");
        if (user.getRole() == null) throw new BusinessRuleException("User role is required");
        if (passwordRequired && (user.getPassword() == null || user.getPassword().isBlank())) {
            throw new BusinessRuleException("Password is required");
        }
    }

    private static String normalizeEmail(String email) { return AirlineService.requireText(email, "Email").toLowerCase(Locale.ROOT); }

    private static String hashPassword(String rawPassword) {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, PASSWORD_ITERATIONS, PASSWORD_KEY_BITS);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return "pbkdf2-sha256$" + PASSWORD_ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
