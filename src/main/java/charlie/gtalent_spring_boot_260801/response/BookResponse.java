package charlie.gtalent_spring_boot_260801.response;

import charlie.gtalent_spring_boot_260801.entity.Book;

public class BookResponse {
    private Long id;

    private String name;

    private Integer price;

    // 目前庫存數量
    private Integer stock;

    // AVAILABLE：還有庫存可以買；SOLD_OUT：已售完
    private String purchaseStatus;

    public BookResponse(Book book) {
        this.id = book.getId();
        this.name = book.getName();
        this.price = book.getPrice();
        this.stock = book.getStock();
        this.purchaseStatus = book.getStock() != null && book.getStock() > 0 ? "AVAILABLE" : "SOLD_OUT";
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    public String getPurchaseStatus() {
        return purchaseStatus;
    }
}
