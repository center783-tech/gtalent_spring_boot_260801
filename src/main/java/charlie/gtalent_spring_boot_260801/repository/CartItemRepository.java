package charlie.gtalent_spring_boot_260801.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import charlie.gtalent_spring_boot_260801.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 依書籍 id 由小到大排序，購物車畫面和結帳扣庫存的順序一致（同時結帳時可避免互相卡住）。
    List<CartItem> findByMemberIdOrderByBookIdAsc(Long memberId);

    Optional<CartItem> findByMemberIdAndBookId(Long memberId, Long bookId);

    // 刪除用的方法必須在交易內呼叫（呼叫端的 service 方法已加 @Transactional）。
    void deleteByMemberId(Long memberId);

    void deleteByMemberIdAndBookId(Long memberId, Long bookId);
}
