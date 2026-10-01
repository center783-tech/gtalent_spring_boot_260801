package charlie.gtalent_spring_boot_260801.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer price;

    //
    @Column(nullable = false)
    private Byte status = 1;

    // 庫存數量：下單時先扣，付款失敗或逾時未付款再還回去。
    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // JPA 從資料庫查資料時會先建立一個空的 Book 物件，再把欄位值塞進來。
    // 這個建構子是給 JPA 用的，所以用 protected，避免一般程式碼直接 new 空書籍。
    protected Book() {
    }

    public Book(String name, Integer price) {
        this.name   = name;
        this.price  = price;
    }

    // 指定庫存的建構子。stock 傳 null 代表「不修改庫存」（修改書籍時使用）。
    public Book(String name, Integer price, Integer stock) {
        this.name   = name;
        this.price  = price;
        this.stock  = stock;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPrice() {
        return this.price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
