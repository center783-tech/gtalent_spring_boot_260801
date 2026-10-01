package charlie.gtalent_spring_boot_260801.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import charlie.gtalent_spring_boot_260801.exception.AuthException;
import charlie.gtalent_spring_boot_260801.interceptor.AuthInterceptor;
import charlie.gtalent_spring_boot_260801.request.CartItemRequest;
import charlie.gtalent_spring_boot_260801.request.CartItemUpdateRequest;
import charlie.gtalent_spring_boot_260801.response.CartResponse;
import charlie.gtalent_spring_boot_260801.service.CartService;
import jakarta.validation.Valid;

// 購物車 API。會員身分來自 AuthInterceptor 放進 request attribute 的 memberId，
// 所以每個人只能操作自己的購物車；這些路徑需要被 AuthInterceptor 攔截（需要登入）。
@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // 取得目前會員的購物車
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public CartResponse getCart(
            @RequestAttribute(name = AuthInterceptor.AUTH_MEMBER_ID_ATTRIBUTE, required = false) Long memberId) {
        return cartService.getCart(requireMemberId(memberId));
    }

    // 加入購物車（同一本書會累加數量）
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.OK)
    public CartResponse addItem(
            @RequestAttribute(name = AuthInterceptor.AUTH_MEMBER_ID_ATTRIBUTE, required = false) Long memberId,
            @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(requireMemberId(memberId), request.getBookId(), request.getQuantity());
    }

    // 修改某一本書的數量
    @PutMapping("/items/{bookId}")
    @ResponseStatus(HttpStatus.OK)
    public CartResponse updateQuantity(
            @RequestAttribute(name = AuthInterceptor.AUTH_MEMBER_ID_ATTRIBUTE, required = false) Long memberId,
            @PathVariable Long bookId,
            @Valid @RequestBody CartItemUpdateRequest request) {
        return cartService.updateQuantity(requireMemberId(memberId), bookId, request.getQuantity());
    }

    // 從購物車移除某一本書
    @DeleteMapping("/items/{bookId}")
    @ResponseStatus(HttpStatus.OK)
    public CartResponse removeItem(
            @RequestAttribute(name = AuthInterceptor.AUTH_MEMBER_ID_ATTRIBUTE, required = false) Long memberId,
            @PathVariable Long bookId) {
        return cartService.removeItem(requireMemberId(memberId), bookId);
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new AuthException("token", ResponseMessages.TOKEN_INVALID);
        }
        return memberId;
    }
}
