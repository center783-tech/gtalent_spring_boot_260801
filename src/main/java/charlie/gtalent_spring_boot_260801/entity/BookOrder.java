package charlie.gtalent_spring_boot_260801.entity;

import java.time.LocalDateTime;

import charlie.gtalent_spring_boot_260801.constant.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "book_orders")
public class BookOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 64, unique = true)
    private String orderNo;

    // 舊資料才有值；新的訂單可以有多本書，書籍資訊改放在 book_order_items。
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "buyer_member_id", nullable = false)
    private Long buyerMemberId;

    // 這張訂單所有書的總本數
    @Column(nullable = false)
    private Integer quantity = 1;

    // 訂單總金額 = 單價 × 數量
    @Column(nullable = false)
    private Integer amount;

    @Column(name = "order_status", nullable = false, length = 32)
    private String orderStatus = OrderStatus.PENDING_PAYMENT;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected BookOrder() {
    }

    // amount 是整張訂單的總金額；每本書的明細在 book_order_items。
    public BookOrder(String orderNo, Long buyerMemberId, Integer quantity, Integer amount) {
        this.orderNo = orderNo;
        this.buyerMemberId = buyerMemberId;
        this.quantity = quantity;
        this.amount = amount;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}