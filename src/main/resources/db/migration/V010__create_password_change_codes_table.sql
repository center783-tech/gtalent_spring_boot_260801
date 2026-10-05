-- 修改密碼前的 Email 驗證碼。
-- DB 只存驗證碼的雜湊（code_hash），不存明碼；attempts 記錄輸入錯誤次數，用來限制亂猜。
-- 注意：這個資料庫帳號沒有 REFERENCES 權限，無法建立外鍵，所以只建立索引。
CREATE TABLE IF NOT EXISTS password_change_codes (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '驗證碼流水號',
    member_id BIGINT NOT NULL COMMENT '會員 ID（對應 members.id）',
    code_hash VARCHAR(64) NOT NULL COMMENT '驗證碼的 SHA-256 雜湊',
    expires_at DATETIME NOT NULL COMMENT '驗證碼到期時間',
    attempts INT NOT NULL DEFAULT 0 COMMENT '已輸入錯誤的次數',
    used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用或作廢：1 是、0 否',
    created_at DATETIME NOT NULL COMMENT '建立（寄出）時間',
    updated_at DATETIME NOT NULL COMMENT '最後更新時間',
    PRIMARY KEY (id),
    KEY idx_password_change_codes_member (member_id, used, expires_at)
) COMMENT='修改密碼的 Email 驗證碼';
