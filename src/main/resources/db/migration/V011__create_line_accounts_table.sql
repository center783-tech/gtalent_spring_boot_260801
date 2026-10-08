-- 管理 LINE 帳號與會員的綁定關係，獨立成一張表，不動 members 本身。
-- 注意：這個資料庫帳號沒有 REFERENCES 權限，無法建立外鍵，所以 member_id 只建立一般索引。
--
-- status：1 = 綁定中，0 = 已解除綁定（保留歷史紀錄，不直接刪除這筆資料）。
-- line_uid 的唯一索引只保證「同一時間」不會有兩筆 status = 1 的一樣的 line_uid；
-- 若之後要支援解除綁定後讓別人綁同一個 LINE，應用程式邏輯要搭配 status 一起判斷
-- （目前的做法是只要曾經綁過就不再釋出，保持最簡單安全）。
CREATE TABLE IF NOT EXISTS line_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水號',
    member_id BIGINT NOT NULL COMMENT '會員 ID（對應 members.id）',
    line_uid VARCHAR(64) NOT NULL COMMENT 'LINE 使用者 ID',
    display_name VARCHAR(100) NULL COMMENT 'LINE 顯示名稱（登入當下的快照，僅供參考/後台顯示）',
    picture_url VARCHAR(255) NULL COMMENT 'LINE 大頭貼網址（登入當下的快照）',
    status_message VARCHAR(255) NULL COMMENT 'LINE 狀態訊息（登入當下的快照）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 = 綁定中，0 = 已解除綁定',
    created_at DATETIME NOT NULL COMMENT '綁定時間',
    updated_at DATETIME NOT NULL COMMENT '最後更新時間（每次用這個 LINE 帳號登入都會更新）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_line_accounts_line_uid (line_uid),
    KEY idx_line_accounts_member (member_id)
) COMMENT='LINE 帳號與會員的綁定關係（LIFF 登入用）';
