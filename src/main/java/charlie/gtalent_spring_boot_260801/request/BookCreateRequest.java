package charlie.gtalent_spring_boot_260801.request;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 新增書籍時的 request body；validation message 放錯誤碼，再由 GlobalExceptionHandler 轉成語系訊息。
public class BookCreateRequest {

    // 書名不可為 null、空字串或只有空白。
    @NotBlank(message = ResponseMessages.BOOK_NAME_REQUIRED)
    private String name;

    // 價格必填，且最小值為 1。
    @NotNull(message = ResponseMessages.BOOK_PRICE_REQUIRED)
    @Min(value = 1, message = ResponseMessages.BOOK_PRICE_MIN)
    private Integer price;

    // 庫存選填；新增時沒填就是 0，修改時沒填就不改庫存。
    @Min(value = 0, message = ResponseMessages.BOOK_STOCK_MIN)
    private Integer stock;

    public Integer getStock() {
        return this.stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPrice() {
        return this.price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

}