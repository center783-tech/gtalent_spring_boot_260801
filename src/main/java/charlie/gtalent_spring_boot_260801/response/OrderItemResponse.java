package charlie.gtalent_spring_boot_260801.response;

import charlie.gtalent_spring_boot_260801.entity.BookOrderItem;
import lombok.Getter;

// 訂單明細中的一本書。書名、單價是下單當下的快照。
@Getter
public class OrderItemResponse {
    private Long bookId;
    private String bookName;
    private Integer unitPrice;
    private Integer quantity;
    private Integer subtotal;

    public OrderItemResponse(BookOrderItem item) {
        this.bookId = item.getBookId();
        this.bookName = item.getBookName();
        this.unitPrice = item.getUnitPrice();
        this.quantity = item.getQuantity();
        this.subtotal = item.getUnitPrice() * item.getQuantity();
    }
}
