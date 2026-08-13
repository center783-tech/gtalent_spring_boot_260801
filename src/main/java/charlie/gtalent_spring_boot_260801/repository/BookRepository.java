package charlie.gtalent_spring_boot_260801.repository;

import java.util.List;

import charlie.gtalent_spring_boot_260801.entity.Book;

public interface BookRepository {

    // 取得所有書籍
     public List<Book> findAll(int page, int size);

    // 新增一本書籍
    public Book create(Book book);

    // 修改一本書籍
    public Book update(Long id,Book book);

    public Book findOneById(Long id);

    // 軟刪除一本書籍
    public void delete(Long id);

    // 取得一本書籍by Name
    public List<Book> findOneByName(String name);

    // 取得書籍總筆數
    public long countAll();

}
