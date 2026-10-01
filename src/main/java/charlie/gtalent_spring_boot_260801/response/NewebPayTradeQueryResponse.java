package charlie.gtalent_spring_boot_260801.response;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class NewebPayTradeQueryResponse {
    private final Long paymentId;
    private final String merchantOrderNo;
    private final String paymentStatus;
    private final String orderStatus;
    private final String tradeStatus;
    private final String providerTradeNo;
    private final String paymentMethod;
    private final String message;
    private final LocalDateTime queriedAt;

    public NewebPayTradeQueryResponse(Long paymentId, String merchantOrderNo, String paymentStatus,
            String orderStatus, String tradeStatus, String providerTradeNo, String paymentMethod,
            String message) {
        this.paymentId = paymentId;
        this.merchantOrderNo = merchantOrderNo;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.tradeStatus = tradeStatus;
        this.providerTradeNo = providerTradeNo;
        this.paymentMethod = paymentMethod;
        this.message = message;
        this.queriedAt = LocalDateTime.now();
    }
}
