package charlie.gtalent_spring_boot_260801.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import charlie.gtalent_spring_boot_260801.entity.Book;
import charlie.gtalent_spring_boot_260801.repository.BookRepository;

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
    public List<Book> getAll() {
        return repository.findAll();
    }

    // 依書名查詢書籍。
    // 範例：GET /books/search?name=Java 入門
    @GetMapping("/search")
    public List<Book> getByName(@RequestParam String name) {
        return repository.findByName(name);
    }

}