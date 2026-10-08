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

/**
 * LINE LIFF authentication endpoints.
 *
 * <p>The actual login and binding logic is handled by {@link MemberService}.
 * This controller is responsible only for request validation and routing.</p>
 */
@RestController
@RequestMapping("/line")
public class LineAuthController {

    private final MemberService memberService;

    public LineAuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * Logs in a LINE account that has already been bound to a member.
     */
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse lineLogin(@Valid @RequestBody LineLoginRequest request) {
        return memberService.lineLogin(request);
    }

    /**
     * 「綁定 LINE 帳號到現有會員；若該帳號尚不存在，則建立新的會員。」
     */
    @PostMapping("/bind")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse lineBind(@Valid @RequestBody LineBindRequest request) {
        return memberService.lineBindOrRegister(request);
    }
}
