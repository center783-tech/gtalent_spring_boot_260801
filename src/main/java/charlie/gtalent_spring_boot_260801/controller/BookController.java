package charlie.gtalent_spring_boot_260801.controller;

import java.util.List;

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

import charlie.gtalent_spring_boot_260801.entity.Book;
import charlie.gtalent_spring_boot_260801.repository.BookRepository;
import charlie.gtalent_spring_boot_260801.request.BookCreateRequest;
import charlie.gtalent_spring_boot_260801.response.ApiResponse;
import charlie.gtalent_spring_boot_260801.response.BookResponse;
import charlie.gtalent_spring_boot_260801.response.PageResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository repository;

    // 注入式
    public BookController(BookRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<BookResponse> getAll(
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
        
        List<Book> books = repository.findAll(page, size);

        // API 不直接回傳 Book Entity，避免把 status、deletedAt 暴露給前端。
        // books.stream()：把 List<Book> 轉成串流，準備逐筆處理。
        // map(BookResponse::new)：每一筆 Book 都執行 new BookResponse(book)，轉成只包含id、name、price  的 DTO。
        // toList()：把轉換後的 BookResponse 收集回 List<BookResponse>。
        List<BookResponse> bookResponses = books.stream()
                .map(BookResponse::new)
                .toList();

        long totalElements = repository.countAll();

        return new PageResponse<>(bookResponses, page, size, totalElements);

    }


    // 取得單一書籍By Id
    @GetMapping("/search-id/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Book getOneById(@PathVariable Long id) {
        Book book = repository.findOneById(id);
        return book;
    }

    // 取得單一書籍By Name
    @GetMapping("search-name/{name}")
    @ResponseStatus(HttpStatus.OK)
    public List<Book> getOneByName(@PathVariable String name) {
        return repository.findOneByName(name);
    }


    // 新增一本書籍
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse create(@Valid @RequestBody BookCreateRequest request) {
        Book book = new Book(request.getName(), request.getPrice());
        repository.create(book);
        return new ApiResponse("新增書籍成功");
    }

    // 修改一本書籍
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody BookCreateRequest request) {
        Book book = new Book(request.getName(), request.getPrice());
        repository.update(id, book);
        return new ApiResponse("修改書籍成功");
    }

    // 軟刪除一本書籍
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse delete(@PathVariable Long id) {
        repository.delete(id);
        return new ApiResponse("刪除書籍成功");
    }
}