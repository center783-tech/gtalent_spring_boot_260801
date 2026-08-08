package charlie.gtalent_spring_boot_260801.repository;

import java.util.List;

import charlie.gtalent_spring_boot_260801.entity.Book;

public interface BookRepository {

    // 取得所有書籍
    public List<Book> findAll();

    // 新增一本書籍
    public Book create(Book book);

    // 修改一本書籍
    public Book update(Long id,Book book);

}
