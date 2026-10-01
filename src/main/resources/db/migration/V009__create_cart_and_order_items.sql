-- 注意：這個資料庫帳號沒有 REFERENCES 權限，無法建立外鍵（FOREIGN KEY），
-- 所以這裡和 V007 一樣只建立索引，資料的對應關係由程式負責維護。
--
-- 購物車：每位會員每本書一筆，數量放在 quantity。
-- 加入購物車不會扣庫存，庫存在結帳（建立訂單）時才扣。
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '購物車項目流水號',
    member_id BIGINT NOT NULL COMMENT '會員 ID（對應 members.id）',
    book_id BIGINT NOT NULL COMMENT '書籍 ID（對應 books.id）',
    quantity INT NOT NULL COMMENT '想購買的數量',
    created_at DATETIME NOT NULL COMMENT '建立時間',
    updated_at DATETIME NOT NULL COMMENT '最後更新時間',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_items_member_book (member_id, book_id),
    KEY idx_cart_items_book_id (book_id)
) COMMENT='購物車；每位會員對每本書最多一筆';

-- 訂單明細：一張訂單可以有多本書。
-- book_name、unit_price 是下單當下的快照，之後書名或價格調整不會影響歷史訂單。
CREATE TABLE IF NOT EXISTS book_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '訂單明細流水號',
    order_id BIGINT NOT NULL COMMENT '對應 book_orders.id',
    book_id BIGINT NOT NULL COMMENT '書籍 ID（對應 books.id）',
    book_name VARCHAR(255) NOT NULL COMMENT '下單當下的書名（快照）',
    unit_price INT NOT NULL COMMENT '下單當下的單價（快照）',
    quantity INT NOT NULL COMMENT '購買數量',
    created_at DATETIME NOT NULL COMMENT '建立時間',
    PRIMARY KEY (id),
    KEY idx_book_order_items_order (order_id),
    KEY idx_book_order_items_book (book_id)
) COMMENT='訂單明細；一張訂單對應多本書';

-- 一張訂單改成可以有多本書，所以訂單本身不再固定對應某一本書。
-- 書籍資訊改由 book_order_items 記錄，book_orders.book_id 只保留給舊資料。
ALTER TABLE book_orders
    MODIFY book_id BIGINT NULL COMMENT '購買的書籍 ID（舊資料才有值；新訂單的書籍記錄在 book_order_items）';

-- 把既有的單本訂單搬進訂單明細。
-- NOT EXISTS 讓這段重跑也不會重複搬同一張訂單；GREATEST 避免數量異常時除以 0。
INSERT INTO book_order_items (order_id, book_id, book_name, unit_price, quantity, created_at)
SELECT o.id, o.book_id, b.name, o.amount DIV GREATEST(o.quantity, 1), o.quantity, o.created_at
FROM book_orders o
JOIN books b ON b.id = o.book_id
WHERE o.book_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM book_order_items i WHERE i.order_id = o.id);
