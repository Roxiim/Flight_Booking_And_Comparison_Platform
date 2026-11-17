package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.*;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.InvalidOperationException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final FlightFareRepository fareRepository;
    private final BookingFlightRepository bookingFlightRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingAddonRepository bookingAddonRepository;
    private final PaymentRepository paymentRepository;

    public Booking getById(Long id) { return bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking", id)); }
    public Booking getByReference(String reference) {
        String code = AirlineService.requireText(reference, "Booking reference");
        return bookingRepository.findByBookingReference(code).orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + code));
    }
    public List<Booking> getHistoryForUser(Long userId) {
        if (!userRepository.existsById(userId)) throw new ResourceNotFoundException("User", userId);
        return bookingRepository.findByUser_Id(userId);
    }
    public Booking getForUser(Long bookingId, Long userId) {
        Booking booking = getById(bookingId);
        if (!booking.getUser().getId().equals(userId)) throw new InvalidOperationException("Booking does not belong to the requested user");
        return booking;
    }

    @Transactional
    public Booking createBooking(Long userId, List<Long> fareIds, String requestedCurrency) {
        if (fareIds == null || fareIds.isEmpty()) throw new BusinessRuleException("At least one flight fare is required");
        if (fareIds.size() != new HashSet<>(fareIds).size()) throw new BusinessRuleException("A fare cannot be selected more than once");
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Map<Long, FlightFare> fares = fareRepository.findAllById(fareIds).stream()
                .collect(Collectors.toMap(FlightFare::getId, Function.identity()));
        if (fares.size() != fareIds.size()) throw new ResourceNotFoundException("One or more selected flight fares do not exist");

        List<FlightFare> selected = fareIds.stream().map(fares::get).toList();
        String currency = normalizeCurrency(requestedCurrency == null ? selected.getFirst().getCurrency() : requestedCurrency);
        BigDecimal total = BigDecimal.ZERO;
        for (FlightFare fare : selected) {
            Flight flight = fare.getFlight();
            if (flight == null || (flight.getStatus() != FlightStatus.SCHEDULED && flight.getStatus() != FlightStatus.DELAYED)) {
                throw new BusinessRuleException("A selected flight is not available for booking");
            }
            if (fare.getPrice() == null || fare.getPrice().signum() < 0) throw new BusinessRuleException("Selected fare has an invalid price");
            if (!normalizeCurrency(fare.getCurrency()).equals(currency)) throw new BusinessRuleException("All selected fares must use the same currency");
            total = total.add(fare.getPrice());
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setBookingReference(generateReference());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCurrency(currency);
        booking.setTotalPrice(total);
        Booking saved = bookingRepository.save(booking);
        List<BookingFlight> bookingFlights = new ArrayList<>();
        for (FlightFare fare : selected) {
            BookingFlight bookingFlight = new BookingFlight();
            bookingFlight.setBooking(saved);
            bookingFlight.setFlight(fare.getFlight());
            bookingFlight.setFare(fare);
            bookingFlight.setPriceAtBooking(fare.getPrice());
            bookingFlights.add(bookingFlight);
        }
        bookingFlightRepository.saveAll(bookingFlights);
        return saved;
    }

    @Transactional
    public Booking updateStatus(Long id, BookingStatus targetStatus) {
        if (targetStatus == null) throw new BusinessRuleException("Booking status is required");
        if (targetStatus == BookingStatus.CANCELLED || targetStatus == BookingStatus.EXPIRED) {
            return transitionToTerminal(id, targetStatus);
        }
        throw new InvalidOperationException("Only payment processing can confirm a booking; pending bookings cannot be reopened");
    }

    @Transactional
    public void cancel(Long id) { transitionToTerminal(id, BookingStatus.CANCELLED); }

    @Transactional
    public Booking recalculateTotal(Long id) {
        Booking booking = bookingRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        if (booking.getStatus() != BookingStatus.PENDING) throw new InvalidOperationException("Only pending bookings can change their total");
        BigDecimal total = bookingFlightRepository.findByBooking_Id(id).stream()
                .map(BookingFlight::getPriceAtBooking).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = total.add(bookingSeatRepository.findByBookingFlight_Booking_Id(id).stream()
                .map(BookingSeat::getSeatPrice).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        total = total.add(bookingAddonRepository.findByBookingFlight_Booking_Id(id).stream()
                .map(addon -> addon.getPriceAtBooking().multiply(BigDecimal.valueOf(addon.getQuantity() == null ? 1 : addon.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        booking.setTotalPrice(total);
        return bookingRepository.save(booking);
    }

    private Booking transitionToTerminal(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        if (status == BookingStatus.EXPIRED && booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidOperationException("Only pending bookings can expire");
        }
        if (status == BookingStatus.CANCELLED && booking.getStatus() == BookingStatus.CONFIRMED) {
            List<BookingFlight> legs = bookingFlightRepository.findByBooking_Id(id);
            if (legs.isEmpty() || legs.stream().anyMatch(leg -> !Boolean.TRUE.equals(leg.getFare().getRefundAllowed()))) {
                throw new InvalidOperationException("Confirmed booking contains a non-refundable fare");
            }
            List<Payment> completedPayments = paymentRepository.findByBooking_Id(id).stream()
                    .filter(payment -> payment.getStatus() == PaymentStatus.COMPLETED).toList();
            if (completedPayments.isEmpty()) throw new InvalidOperationException("Confirmed booking has no completed payment to refund");
            completedPayments.forEach(payment -> {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setPaymentDate(java.time.LocalDateTime.now());
            });
        } else if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidOperationException("Only pending bookings or refundable confirmed bookings can be cancelled");
        }
        booking.setStatus(status);
        bookingSeatRepository.deleteByBookingFlight_Booking_Id(id);
        paymentRepository.findByBooking_Id(id).stream().filter(payment -> payment.getStatus() == PaymentStatus.PENDING).forEach(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setPaymentDate(java.time.LocalDateTime.now());
        });
        return bookingRepository.save(booking);
    }

    private String generateReference() {
        String reference;
        do { reference = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(Locale.ROOT); }
        while (bookingRepository.existsByBookingReference(reference));
        return reference;
    }

    private static String normalizeCurrency(String currency) {
        String result = currency == null || currency.isBlank() ? "EUR" : currency.trim().toUpperCase(Locale.ROOT);
        if (result.length() != 3) throw new BusinessRuleException("Currency must contain exactly three characters");
        return result;
    }
}
