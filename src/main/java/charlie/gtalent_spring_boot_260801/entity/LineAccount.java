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

// LINE 帳號與會員的綁定關係（LIFF 登入用），獨立成一張表，不碰 Member 本身。
// 一個 member_id 原則上只會有一筆 status = 1 的紀錄（一個會員只能綁一個 LINE 帳號）。
@Getter
@Setter
@Entity
@Table(name = "line_accounts")
public class LineAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "line_uid", nullable = false, length = 64, unique = true)
    private String lineUid;

    // 下面三個都只是登入當下的快照，給後台顯示用；不是登入判斷的依據。
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "picture_url", length = 255)
    private String pictureUrl;

    @Column(name = "status_message", length = 255)
    private String statusMessage;

    // 1 = 綁定中，0 = 已解除綁定（保留歷史紀錄）
    @Column(nullable = false)
    private Byte status = 1;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LineAccount() {
    }

    public LineAccount(Long memberId, String lineUid, String displayName, String pictureUrl, String statusMessage) {
        this.memberId = memberId;
        this.lineUid = lineUid;
        this.displayName = displayName;
        this.pictureUrl = pictureUrl;
        this.statusMessage = statusMessage;
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
