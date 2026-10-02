package charlie.gtalent_spring_boot_260801.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import charlie.gtalent_spring_boot_260801.entity.BookOrderItem;

public interface BookOrderItemRepository extends JpaRepository<BookOrderItem, Long> {

    List<BookOrderItem> findByOrderId(Long orderId);

    // 一次取得多張訂單的明細，列表頁用它避免每張訂單各查一次。
    List<BookOrderItem> findByOrderIdIn(Collection<Long> orderIds);
}
