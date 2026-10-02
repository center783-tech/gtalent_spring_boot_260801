package charlie.gtalent_spring_boot_260801.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import charlie.gtalent_spring_boot_260801.exception.AuthException;
import charlie.gtalent_spring_boot_260801.interceptor.AuthInterceptor;
import charlie.gtalent_spring_boot_260801.response.OrderResponse;
import charlie.gtalent_spring_boot_260801.response.PageResponse;
import charlie.gtalent_spring_boot_260801.service.OrderQueryService;

// 會員的購買紀錄。會員身分來自 AuthInterceptor 放進 request attribute 的 memberId，
// 所以每個人只查得到自己的訂單；這個路徑需要被 AuthInterceptor 攔截（需要登入）。
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderQueryService orderQueryService;

    public OrderController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    // 查詢自己的購買紀錄（新的在前）。條件都是選填：
    // status 訂單狀態、orderNo 訂單編號（模糊比對）、from / to 日期範圍（yyyy-MM-dd，含當天）。
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<OrderResponse> getMyOrders(
            @RequestAttribute(name = AuthInterceptor.AUTH_MEMBER_ID_ATTRIBUTE, required = false) Long memberId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (memberId == null) {
            throw new AuthException("token", ResponseMessages.TOKEN_INVALID);
        }

        // 頁碼從 1 開始；每頁最少 1 筆、最多 50 筆（與書籍列表相同）。
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        if (size > 50) {
            size = 50;
        }

        return orderQueryService.search(memberId, page, size, status, orderNo, from, to);
    }
}
