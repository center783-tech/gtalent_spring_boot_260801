package charlie.gtalent_spring_boot_260801.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import charlie.gtalent_spring_boot_260801.entity.BookOrderItem;

public interface BookOrderItemRepository extends JpaRepository<BookOrderItem, Long> {

    List<BookOrderItem> findByOrderId(Long orderId);
}
