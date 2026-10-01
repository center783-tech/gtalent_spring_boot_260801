package charlie.gtalent_spring_boot_260801.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import charlie.gtalent_spring_boot_260801.constant.OrderStatus;
import charlie.gtalent_spring_boot_260801.constant.PaymentStatus;
import charlie.gtalent_spring_boot_260801.entity.BookOrder;
import charlie.gtalent_spring_boot_260801.repository.BookOrderRepository;
import charlie.gtalent_spring_boot_260801.repository.PaymentRepository;

// 定時取消「下單後超過期限仍未付款」的訂單，並把下單時保留的庫存還回去。
// 使用者關掉付款頁就離開時，藍新不會通知我們，所以需要這個背景工作來釋放庫存。
@Service
public class OrderExpirationService {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationService.class);

    private final BookOrderRepository bookOrderRepository;
    private final PaymentRepository paymentRepository;
    private final BookOrderService bookOrderService;
    private final TransactionTemplate transactionTemplate;
    private final int paymentTimeoutMinutes;

    public OrderExpirationService(
            BookOrderRepository bookOrderRepository,
            PaymentRepository paymentRepository,
            BookOrderService bookOrderService,
            PlatformTransactionManager transactionManager,
            @Value("${book-order.payment-timeout-minutes:30}") int paymentTimeoutMinutes) {
        this.bookOrderRepository = bookOrderRepository;
        this.paymentRepository = paymentRepository;
        this.bookOrderService = bookOrderService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.paymentTimeoutMinutes = paymentTimeoutMinutes;
    }

    // 每次執行結束後，間隔 1 分鐘再執行下一次（可用 book-order.expire-check-interval-ms 調整）。
    @Scheduled(fixedDelayString = "${book-order.expire-check-interval-ms:60000}")
    public void cancelExpiredOrders() {
        LocalDateTime expireBefore = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
        List<BookOrder> expiredOrders = bookOrderRepository
                .findByOrderStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, expireBefore);

        for (BookOrder order : expiredOrders) {
            // 每一筆各自一個交易：某一筆失敗不會影響其他筆。
            try {
                cancelOrder(order);
            } catch (RuntimeException exception) {
                log.error("取消逾時訂單失敗，orderNo={}", order.getOrderNo(), exception);
            }
        }
    }

    private void cancelOrder(BookOrder order) {
        transactionTemplate.executeWithoutResult(status -> {
            // 只有訂單還是待付款才取消；若剛好已付款或已被處理，受影響筆數會是 0，就什麼都不做。
            int updated = bookOrderRepository.cancelIfPending(
                    order.getId(), LocalDateTime.now(), OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED);
            if (updated == 0) {
                return;
            }

            // 付款單的 merchantOrderNo 就是訂單編號。付款單改成已取消，
            // 之後藍新若還送通知來，handleNotify 會因為付款單不是 PENDING 而拒絕。
            paymentRepository.findByMerchantOrderNo(order.getOrderNo()).ifPresent(payment -> {
                if (!PaymentStatus.PAID.equals(payment.getPaymentStatus())) {
                    payment.setPaymentStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(payment);
                }
            });

            // 把下單時保留的庫存還回去（訂單裡每一本書都要還）。
            bookOrderService.restoreStock(order.getId());
            log.info("訂單逾時未付款，已取消並還回庫存，orderNo={}", order.getOrderNo());
        });
    }
}
