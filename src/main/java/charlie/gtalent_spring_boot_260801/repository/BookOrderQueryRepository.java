package charlie.gtalent_spring_boot_260801.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import charlie.gtalent_spring_boot_260801.entity.BookOrder;

// 購買紀錄的查詢專用 repository：查詢條件（狀態、訂單編號、日期）是動態組合的，
// 所以用 Specification，只加上使用者有填的條件。
public interface BookOrderQueryRepository
        extends JpaRepository<BookOrder, Long>, JpaSpecificationExecutor<BookOrder> {
}
