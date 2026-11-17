package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.User;
import com.flightplatform.backend.entity.UserRole;
import com.flightplatform.backend.exception.DuplicateResourceException;
import com.flightplatform.backend.repository.AirlineRepository;
import com.flightplatform.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock AirlineRepository airlines;
    @InjectMocks UserService service;

    @Test void createsUserWithNormalizedEmailAndHashedPassword() {
        User input = user();
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.empty());
        when(users.save(input)).thenReturn(input);
        User saved = service.create(input);
        assertEquals("student@example.com", saved.getEmail());
        assertNotEquals("Long password!", saved.getPassword());
        assertTrue(saved.getPassword().startsWith("pbkdf2-sha256$"));
        verify(users).save(input);
    }

    @Test void rejectsDuplicateEmail() {
        User existing = user(); existing.setId(2L);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(existing));
        assertThrows(DuplicateResourceException.class, () -> service.create(user()));
        verify(users, never()).save(any());
    }

    @Test void updateCanLeavePasswordUnchanged() {
        User existing = user(); existing.setId(1L); existing.setPassword("existing-hash");
        User changes = user(); changes.setPassword(null);
        when(users.findById(1L)).thenReturn(Optional.of(existing));
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(existing));
        when(users.save(existing)).thenReturn(existing);
        assertEquals("existing-hash", service.update(1L, changes).getPassword());
    }

    private static User user() {
        User user = new User(); user.setFirstName("Test"); user.setLastName("Student");
        user.setEmail(" Student@Example.com "); user.setPassword("Long password!"); user.setRole(UserRole.CUSTOMER); return user;
    }
}
