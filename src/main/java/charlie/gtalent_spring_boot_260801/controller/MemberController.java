package charlie.gtalent_spring_boot_260801.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import charlie.gtalent_spring_boot_260801.request.MemberLoginRequest;
import charlie.gtalent_spring_boot_260801.request.MemberPasswordUpdateRequest;
import charlie.gtalent_spring_boot_260801.request.MemberProfileUpdateRequest;
import charlie.gtalent_spring_boot_260801.request.MemberRegisterRequest;
import charlie.gtalent_spring_boot_260801.request.TokenLogoutRequest;
import charlie.gtalent_spring_boot_260801.request.TokenRefreshRequest;
import charlie.gtalent_spring_boot_260801.response.ApiResponse;
import charlie.gtalent_spring_boot_260801.response.MemberResponse;
import charlie.gtalent_spring_boot_260801.response.PageResponse;
import charlie.gtalent_spring_boot_260801.response.TokenResponse;
import charlie.gtalent_spring_boot_260801.service.MemberService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MemberResponse getOneById(@PathVariable Long id) {
        return new MemberResponse(memberService.findOneById(id));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse register(@Valid @RequestBody MemberRegisterRequest request) {
        memberService.register(request);
        return new ApiResponse("會員註冊成功");
    }

    @PutMapping("/{id}/profile")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody MemberProfileUpdateRequest request) {
        memberService.updateProfile(id, request);
        return new ApiResponse("會員基本資料修改成功");
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody MemberPasswordUpdateRequest request) {
        memberService.updatePassword(id, request);
        return new ApiResponse("會員密碼修改成功");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse delete(@PathVariable Long id) {
        memberService.delete(id);
        return new ApiResponse("會員帳號刪除成功");
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse login(@Valid @RequestBody MemberLoginRequest request) {
        return memberService.login(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return memberService.refresh(request.getRefreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse logout(@Valid @RequestBody TokenLogoutRequest request) {
        memberService.logout(request.getRefreshToken());
        return new ApiResponse("會員登出成功");
    }
    // 取得所有的會員
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<MemberResponse> getAll(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        
         // 預設頁碼從1開始
        if(page < 1) {
            page = 1;
        }

        // 每頁最少數量不能為0
        // 如果帶0進來, 自動呈現1頁10組
        if(size < 1) {
            size = 10;
        }

        // 每頁最大不能超過50組
        if (size > 50) {
            size = 50;
        }
        
        return memberService.getAll(page, size); 
        
        }
    // 課後練習:
    // 1. get members 取得所有會員 且 做分頁功能
}
