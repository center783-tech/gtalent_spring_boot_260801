package charlie.gtalent_spring_boot_260801.request;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// LIFF 登入第二步：第一步用 lineUid 找不到會員時，請使用者輸入既有的帳號密碼，
// 連同 lineUid 一起送到後端。後端會判斷是「綁定既有帳號」還是「用這組帳號密碼新建會員」。
// displayName / pictureUrl / statusMessage 都是選填：新建會員時 displayName 當預設姓名，
// 三個欄位也會一起存進 line_accounts 當作快照。
@Getter
@Setter
public class LineBindRequest {

    @NotBlank(message = ResponseMessages.LINE_UID_REQUIRED)
    private String lineUid;

    @NotBlank(message = ResponseMessages.MEMBER_ACCOUNT_REQUIRED)
    @Size(max = 30, message = ResponseMessages.MEMBER_ACCOUNT_MAX)
    private String account;

    @NotBlank(message = ResponseMessages.MEMBER_PASSWORD_REQUIRED)
    @Size(max = 12, min = 6, message = ResponseMessages.MEMBER_PASSWORD_SIZE)
    private String password;

    @Size(max = 30, message = ResponseMessages.MEMBER_NAME_MAX)
    private String displayName;

    private String pictureUrl;
    private String statusMessage;
}
