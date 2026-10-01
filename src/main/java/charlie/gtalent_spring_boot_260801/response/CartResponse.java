package charlie.gtalent_spring_boot_260801.response;

import java.util.List;

import lombok.Getter;

@Getter
public class CartResponse {
    private List<CartItemResponse> items;

    // 購物車內所有書的總本數
    private int totalQuantity;

    // 總金額（已下架的書不計入）
    private int totalAmount;

    // 可以結帳：購物車不是空的，而且每一筆都還有足夠庫存。
    private boolean checkoutAllowed;

    public CartResponse(List<CartItemResponse> items) {
        this.items = items;
        for (CartItemResponse item : items) {
            this.totalQuantity += item.getQuantity();
            this.totalAmount += item.getSubtotal();
        }
        this.checkoutAllowed = !items.isEmpty() && items.stream().allMatch(CartItemResponse::isAvailable);
    }
}
