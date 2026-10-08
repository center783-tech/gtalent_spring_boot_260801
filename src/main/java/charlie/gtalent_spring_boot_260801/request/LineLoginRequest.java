package charlie.gtalent_spring_boot_260801.request;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// LIFF 登入第一步：帶 LINE 使用者 ID，看看這個 LINE 帳號有沒有綁定過會員。
// displayName / pictureUrl / statusMessage 都是選填，有帶的話會順便更新 line_accounts 的快照，
// 不影響登入判斷本身（判斷只看 lineUid）。
@Getter
@Setter
public class LineLoginRequest {

    @NotBlank(message = ResponseMessages.LINE_UID_REQUIRED)
    private String lineUid;

    private String displayName;
    private String pictureUrl;
    private String statusMessage;
}
