package charlie.gtalent_spring_boot_260801.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import charlie.gtalent_spring_boot_260801.entity.Book;
import charlie.gtalent_spring_boot_260801.repository.BookRepository;
import charlie.gtalent_spring_boot_260801.request.BookCreateRequest;
import charlie.gtalent_spring_boot_260801.response.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository repository;

    // 注入式
    public BookController(BookRepository repository) {
        this.repository = repository;
    }

    // 取得所有的書籍
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Book> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Book getBook(@PathVariable Long id) {
    return repository.findById(id);
}
    

    // 新增一本書籍
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody BookCreateRequest request) {
        Book book = new Book(request.getName(), request.getPrice());
        repository.create(book);
        URI location = URI.create("/books/" + book.getId());
        return ResponseEntity.created(location).body(new ApiResponse("新增書籍成功"));
    }

    // 修改一本書籍
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody BookCreateRequest request) {
        Book book = new Book(request.getName(), request.getPrice());
        repository.update(id, book);
        return new ApiResponse("修改書籍成功");
    }
//     @Bean
//     public CommandLineRunner printEndpoints(ApplicationContext ctx) {
//     return args -> {
//         RequestMappingHandlerMapping mapping = ctx.getBean(RequestMappingHandlerMapping.class);
//         mapping.getHandlerMethods().forEach((key, value) -> {
//             System.out.println(key + " : " + value);
//         });
//     };
// }
}