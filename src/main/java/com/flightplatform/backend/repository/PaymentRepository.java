package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBooking_Id(Long bookingId);
    boolean existsByBooking_IdAndStatus(Long bookingId, com.flightplatform.backend.entity.PaymentStatus status);
    Optional<Payment> findByTransactionReference(String transactionReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);
}
