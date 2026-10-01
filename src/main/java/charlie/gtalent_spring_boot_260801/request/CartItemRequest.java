package charlie.gtalent_spring_boot_260801.request;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 加入購物車的 request body。
public class CartItemRequest {

    @NotNull(message = ResponseMessages.CART_BOOK_ID_REQUIRED)
    private Long bookId;

    @NotNull(message = ResponseMessages.CART_QUANTITY_REQUIRED)
    @Min(value = 1, message = ResponseMessages.ORDER_QUANTITY_INVALID)
    private Integer quantity;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
