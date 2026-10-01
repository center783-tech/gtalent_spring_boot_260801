package charlie.gtalent_spring_boot_260801.request;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 修改購物車商品數量的 request body（書籍 id 在網址上）。
public class CartItemUpdateRequest {

    @NotNull(message = ResponseMessages.CART_QUANTITY_REQUIRED)
    @Min(value = 1, message = ResponseMessages.ORDER_QUANTITY_INVALID)
    private Integer quantity;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
