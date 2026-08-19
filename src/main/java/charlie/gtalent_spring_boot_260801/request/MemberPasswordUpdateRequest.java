package charlie.gtalent_spring_boot_260801.request;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberPasswordUpdateRequest {
    @NotBlank(message = ResponseMessages.MEMBER_PASSWORD_REQUIRED)
        @Size(max = 12, min = 6, message = ResponseMessages.MEMBER_PASSWORD_SIZE)
        private String password;

    @NotBlank(message = ResponseMessages.MEMBER_CONFIRM_PASSWORD_REQUIRED)
        private String confirmPassword;
}
