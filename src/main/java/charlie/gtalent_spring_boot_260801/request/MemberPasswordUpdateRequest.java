package charlie.gtalent_spring_boot_260801.request;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberPasswordUpdateRequest {

    // 修改密碼前，先請系統寄驗證碼到會員信箱，再把信裡的 6 位數驗證碼帶在這裡。
    @NotBlank(message = ResponseMessages.PASSWORD_CHANGE_CODE_REQUIRED)
    @Pattern(regexp = "\\d{6}", message = ResponseMessages.PASSWORD_CHANGE_CODE_REQUIRED)
    private String verificationCode;

    @NotBlank(message = ResponseMessages.MEMBER_PASSWORD_REQUIRED)
    @Size(max = 12, min = 6, message = ResponseMessages.MEMBER_PASSWORD_SIZE)
    private String password;

    @NotBlank(message = ResponseMessages.MEMBER_CONFIRM_PASSWORD_REQUIRED)
    private String confirmPassword;
}
