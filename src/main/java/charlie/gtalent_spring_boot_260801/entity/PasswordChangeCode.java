package charlie.gtalent_spring_boot_260801.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

// 修改密碼前寄到會員信箱的驗證碼。DB 只存雜湊，不存驗證碼本身。
@Getter
@Setter
@Entity
@Table(name = "password_change_codes")
public class PasswordChangeCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 已輸入錯誤的次數；超過上限就作廢，不能再嘗試。
    @Column(nullable = false)
    private Integer attempts = 0;

    // 0 = 可使用，1 = 已使用或已作廢
    @Column(nullable = false)
    private Byte used = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PasswordChangeCode() {
    }

    public PasswordChangeCode(Long memberId, String codeHash, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
