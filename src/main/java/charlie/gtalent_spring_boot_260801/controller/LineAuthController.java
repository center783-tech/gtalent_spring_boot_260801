package charlie.gtalent_spring_boot_260801.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import charlie.gtalent_spring_boot_260801.request.LineBindRequest;
import charlie.gtalent_spring_boot_260801.request.LineLoginRequest;
import charlie.gtalent_spring_boot_260801.response.TokenResponse;
import charlie.gtalent_spring_boot_260801.service.MemberService;
import jakarta.validation.Valid;

// LIFF（LINE 內建瀏覽器）登入用的 API。
// 這兩支和 /members/login 一樣是登入流程本身，不需要先登入，AuthInterceptor 不應該攔截 /line/**。
@RestController
@RequestMapping("/line")
public class LineAuthController {

    private final MemberService memberService;

    public LineAuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 步驟 1：只帶 LINE 使用者 ID 嘗試登入。
    // 找到已綁定的會員就回 200 + token；找不到則由 GlobalExceptionHandler 回 404（MEMBER_NOT_FOUND），
    // 前端收到 404 就改顯示帳號密碼輸入畫面，呼叫下面的 /line/bind。
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse lineLogin(@Valid @RequestBody LineLoginRequest request) {
        return memberService.lineLogin(request);
    }

    // 步驟 2：帶著 lineUid + 既有帳號密碼，綁定既有會員或新建會員。
    @PostMapping("/bind")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse lineBind(@Valid @RequestBody LineBindRequest request) {
        return memberService.lineBindOrRegister(request);
    }
}
