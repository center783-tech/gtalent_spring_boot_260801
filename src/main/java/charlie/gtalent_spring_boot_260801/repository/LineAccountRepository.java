package charlie.gtalent_spring_boot_260801.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import charlie.gtalent_spring_boot_260801.entity.LineAccount;

public interface LineAccountRepository extends JpaRepository<LineAccount, Long> {

    // LIFF 登入第一步：用 LINE 使用者 ID 找目前綁定中的紀錄，進而找出對應的 member_id。
    @Query(
        value = "SELECT * FROM line_accounts WHERE line_uid = :lineUid AND status = 1",
        nativeQuery = true
    )
    Optional<LineAccount> findActiveByLineUid(@Param("lineUid") String lineUid);

    // 這個會員目前是否已經綁定過某個 LINE 帳號（一個會員只能綁一個）。
    @Query(
        value = "SELECT * FROM line_accounts WHERE member_id = :memberId AND status = 1",
        nativeQuery = true
    )
    Optional<LineAccount> findActiveByMemberId(@Param("memberId") Long memberId);
}
