package charlie.gtalent_spring_boot_260801.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

// 統一 API 回應格式：message 放大項目訊息，errors 放欄位錯誤訊息。
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    private String message;

    private Map<String, String> errors;

    public ApiResponse(String message) {
        this.message = message;
    }

    public ApiResponse(String message, Map<String, String> errors) {
        this.message = message;
        this.errors = errors;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    

}