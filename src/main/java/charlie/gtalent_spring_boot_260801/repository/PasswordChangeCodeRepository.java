package charlie.gtalent_spring_boot_260801.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import charlie.gtalent_spring_boot_260801.entity.PasswordChangeCode;

public interface PasswordChangeCodeRepository extends JpaRepository<PasswordChangeCode, Long> {

    // 這位會員最新一筆驗證碼（不管有沒有用過），用來判斷是否還在「重新寄送」的冷卻時間內。
    Optional<PasswordChangeCode> findFirstByMemberIdOrderByIdDesc(Long memberId);

    // 這位會員最新一筆「可使用」的驗證碼（used = 0）。
    Optional<PasswordChangeCode> findFirstByMemberIdAndUsedOrderByIdDesc(Long memberId, Byte used);

    // 這位會員所有「可使用」的驗證碼，重新寄送時要全部作廢。
    List<PasswordChangeCode> findByMemberIdAndUsed(Long memberId, Byte used);
}
