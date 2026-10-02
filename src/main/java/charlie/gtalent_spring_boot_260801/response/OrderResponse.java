package charlie.gtalent_spring_boot_260801.response;

import java.time.LocalDateTime;
import java.util.List;

import charlie.gtalent_spring_boot_260801.entity.BookOrder;
import lombok.Getter;

// 購買紀錄中的一張訂單，包含訂單裡的每一本書。
@Getter
public class OrderResponse {
    private Long orderId;
    private String orderNo;

    // PENDING_PAYMENT 待付款、PAID 已付款、CANCELLED 已取消、FAILED 付款失敗
    private String orderStatus;

    // 這張訂單所有書的總本數
    private Integer quantity;

    // 訂單總金額
    private Integer amount;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private List<OrderItemResponse> items;

    public OrderResponse(BookOrder order, List<OrderItemResponse> items) {
        this.orderId = order.getId();
        this.orderNo = order.getOrderNo();
        this.orderStatus = order.getOrderStatus();
        this.quantity = order.getQuantity();
        this.amount = order.getAmount();
        this.createdAt = order.getCreatedAt();
        this.paidAt = order.getPaidAt();
        this.cancelledAt = order.getCancelledAt();
        this.items = items;
    }
}
