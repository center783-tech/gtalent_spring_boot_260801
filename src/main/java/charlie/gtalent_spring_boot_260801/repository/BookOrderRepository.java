package charlie.gtalent_spring_boot_260801.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import charlie.gtalent_spring_boot_260801.entity.BookOrder;

public interface BookOrderRepository extends JpaRepository<BookOrder, Long> {

    @Query(
        value = "SELECT COUNT(*) FROM book_orders WHERE book_id = :bookId AND order_status = :orderStatus",
        nativeQuery = true
    )
    long countByBookIdAndOrderStatus(
        @Param("bookId") Long bookId,
        @Param("orderStatus") String orderStatus
    );

    @Query(
        value = "SELECT COUNT(*) FROM book_orders WHERE order_no = :orderNo",
        nativeQuery = true
    )
    long countByOrderNo(@Param("orderNo") String orderNo);

    // 找出建立時間早於 createdAt、且狀態為 orderStatus 的訂單（用來找逾時未付款的訂單）。
    List<BookOrder> findByOrderStatusAndCreatedAtBefore(String orderStatus, LocalDateTime createdAt);

    // 只有訂單「還是待付款」時才改成已取消，回傳受影響筆數。
    // 條件放在 WHERE 裡，這樣就算同一時間藍新剛好通知付款成功，也不會把已付款的訂單誤取消。
    @Modifying
    @Query(
        value = "UPDATE book_orders SET order_status = :cancelled, cancelled_at = :now, updated_at = :now "
              + "WHERE id = :id AND order_status = :pending",
        nativeQuery = true
    )
    int cancelIfPending(
        @Param("id") Long id,
        @Param("now") LocalDateTime now,
        @Param("pending") String pending,
        @Param("cancelled") String cancelled
    );
}