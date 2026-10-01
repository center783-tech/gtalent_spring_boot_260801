package charlie.gtalent_spring_boot_260801.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import charlie.gtalent_spring_boot_260801.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByMerchantOrderNo(String merchantOrderNo);

    List<Payment> findByPaymentStatusAndUpdatedAtBefore(String paymentStatus, LocalDateTime updatedAt);
}
