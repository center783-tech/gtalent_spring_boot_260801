package charlie.gtalent_spring_boot_260801.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import charlie.gtalent_spring_boot_260801.entity.PaymentNotification;

public interface PaymentNotificationRepository extends JpaRepository<PaymentNotification, Long> {

    @Query(
            value = """
                    SELECT * FROM payment_notifications
                    WHERE merchant_order_no = :merchantOrderNo
                    ORDER BY id DESC
                    """,
            nativeQuery = true
    )
    List<PaymentNotification> findByMerchantOrderNoOrderByIdDesc(
            @Param("merchantOrderNo") String merchantOrderNo
    );
}