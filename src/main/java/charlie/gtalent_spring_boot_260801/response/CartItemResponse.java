package charlie.gtalent_spring_boot_260801.response;

import charlie.gtalent_spring_boot_260801.entity.Book;
import charlie.gtalent_spring_boot_260801.entity.CartItem;
import lombok.Getter;

// 購物車裡的一筆商品，包含書籍目前的名稱、價格、庫存。
@Getter
public class CartItemResponse {
    private Long bookId;
    private String name;
    private Integer price;
    private Integer quantity;
    private Integer stock;
    private Integer subtotal;

    // true：書籍還在，而且庫存足夠；false：書籍已下架，或庫存比購物車數量少。
    private boolean available;

    // book 為 null 代表這本書已被刪除（下架）。
    public CartItemResponse(CartItem item, Book book) {
        this.bookId = item.getBookId();
        this.quantity = item.getQuantity();

        if (book == null) {
            this.name = "（書籍已下架）";
            this.price = 0;
            this.stock = 0;
            this.subtotal = 0;
            this.available = false;
            return;
        }

        this.name = book.getName();
        this.price = book.getPrice();
        this.stock = book.getStock();
        this.subtotal = book.getPrice() * item.getQuantity();
        this.available = book.getStock() >= item.getQuantity();
    }
}
