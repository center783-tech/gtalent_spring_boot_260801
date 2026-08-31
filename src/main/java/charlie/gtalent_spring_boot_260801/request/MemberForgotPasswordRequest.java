package charlie.gtalent_spring_boot_260801.request;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberForgotPasswordRequest {
    
    @NotBlank(message = ResponseMessages.MEMBER_ACCOUNT_OR_EMAIL_REQUIRED)
    @Size(max = 128, message = ResponseMessages.MEMBER_ACCOUNT_OR_EMAIL_MAX)
    private String accountOrEmail;
    
}
