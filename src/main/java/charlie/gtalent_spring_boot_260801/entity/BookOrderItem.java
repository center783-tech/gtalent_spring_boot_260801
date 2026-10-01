package charlie.gtalent_spring_boot_260801.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// 訂單明細：一張訂單裡的某一本書。
// bookName、unitPrice 是下單當下的快照，之後書名或價格調整不會影響歷史訂單。
@Getter
@Setter
@Entity
@Table(name = "book_order_items")
public class BookOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "book_name", nullable = false)
    private String bookName;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected BookOrderItem() {
    }

    public BookOrderItem(Long orderId, Long bookId, String bookName, Integer unitPrice, Integer quantity) {
        this.orderId = orderId;
        this.bookId = bookId;
        this.bookName = bookName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
