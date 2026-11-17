package com.flightplatform.backend.service;

import com.flightplatform.backend.entity.Booking;
import com.flightplatform.backend.entity.BookingStatus;
import com.flightplatform.backend.entity.Payment;
import com.flightplatform.backend.entity.PaymentMethod;
import com.flightplatform.backend.entity.PaymentStatus;
import com.flightplatform.backend.exception.BusinessRuleException;
import com.flightplatform.backend.exception.InvalidOperationException;
import com.flightplatform.backend.exception.ResourceNotFoundException;
import com.flightplatform.backend.repository.BookingRepository;
import com.flightplatform.backend.repository.BookingFlightRepository;
import com.flightplatform.backend.repository.PassengerRepository;
import com.flightplatform.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingFlightRepository bookingFlightRepository;
    private final PassengerRepository passengerRepository;

    public Payment getById(Long id) { return paymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment", id)); }
    public List<Payment> getByBooking(Long bookingId) {
        if (!bookingRepository.existsById(bookingId)) throw new ResourceNotFoundException("Booking", bookingId);
        return paymentRepository.findByBooking_Id(bookingId);
    }

    @Transactional
    public Payment createPayment(Long bookingId, PaymentMethod method) {
        if (method == null) throw new BusinessRuleException("Payment method is required");
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (booking.getStatus() != BookingStatus.PENDING) throw new InvalidOperationException("Only pending bookings can be paid");
        if (bookingFlightRepository.countByBooking_Id(bookingId) == 0) throw new BusinessRuleException("Booking must include a flight before payment");
        if (passengerRepository.countByBooking_Id(bookingId) == 0) throw new BusinessRuleException("At least one passenger is required before payment");
        if (paymentRepository.existsByBooking_IdAndStatus(bookingId, PaymentStatus.PENDING)) {
            throw new InvalidOperationException("A payment is already pending for this booking");
        }
        if (paymentRepository.existsByBooking_IdAndStatus(bookingId, PaymentStatus.COMPLETED)) {
            throw new InvalidOperationException("Booking has already been paid successfully");
        }
        BigDecimal amount = booking.getTotalPrice();
        if (amount == null || amount.signum() < 0) throw new BusinessRuleException("Booking total is invalid");
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setCurrency(booking.getCurrency() == null ? "EUR" : booking.getCurrency());
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionReference("SIM-" + UUID.randomUUID());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment processSimulatedPayment(Long paymentId, boolean successful) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        Booking booking = bookingRepository.findByIdForUpdate(payment.getBooking().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", payment.getBooking().getId()));
        if (payment.getStatus() != PaymentStatus.PENDING) throw new InvalidOperationException("Only pending payments can be processed");
        if (booking.getStatus() != BookingStatus.PENDING) throw new InvalidOperationException("Booking is no longer pending");
        if (successful && paymentRepository.existsByBooking_IdAndStatus(booking.getId(), PaymentStatus.COMPLETED)) {
            throw new InvalidOperationException("Booking already has a successful payment");
        }
        payment.setStatus(successful ? PaymentStatus.COMPLETED : PaymentStatus.FAILED);
        payment.setPaymentDate(LocalDateTime.now());
        if (successful) booking.setStatus(BookingStatus.CONFIRMED);
        paymentRepository.save(payment);
        bookingRepository.save(booking);
        return payment;
    }
}
