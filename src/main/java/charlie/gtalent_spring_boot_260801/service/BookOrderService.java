package charlie.gtalent_spring_boot_260801.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import charlie.gtalent_spring_boot_260801.entity.Book;
import charlie.gtalent_spring_boot_260801.entity.BookOrder;
import charlie.gtalent_spring_boot_260801.entity.BookOrderItem;
import charlie.gtalent_spring_boot_260801.entity.CartItem;
import charlie.gtalent_spring_boot_260801.entity.Payment;
import charlie.gtalent_spring_boot_260801.exception.BookOrderException;
import charlie.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import charlie.gtalent_spring_boot_260801.repository.BookOrderItemRepository;
import charlie.gtalent_spring_boot_260801.repository.BookOrderRepository;
import charlie.gtalent_spring_boot_260801.repository.BookRepository;
import charlie.gtalent_spring_boot_260801.repository.CartItemRepository;
import charlie.gtalent_spring_boot_260801.repository.PaymentRepository;
import charlie.gtalent_spring_boot_260801.response.BookOrderCreateResponse;
import jakarta.persistence.NoResultException;

@Service
public class BookOrderService {

    // 藍新 ItemDesc 最多 50 個字。
    private static final int ITEM_DESC_MAX_LENGTH = 50;

    // 訂單中的一筆：要買哪一本書、買幾本。
    private record OrderLine(Book book, int quantity) {
    }

    private final BookRepository bookRepository;
    private final BookOrderRepository bookOrderRepository;
    private final BookOrderItemRepository bookOrderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final String newebpayMerchantId;

    public BookOrderService(
        BookRepository bookRepository,
        BookOrderRepository bookOrderRepository,
        BookOrderItemRepository bookOrderItemRepository,
        CartItemRepository cartItemRepository,
        PaymentRepository paymentRepository,
        @Value("${newebpay.merchant-id}") String newebpayMerchantId) {
            this.bookRepository = bookRepository;
            this.bookOrderRepository = bookOrderRepository;
            this.bookOrderItemRepository = bookOrderItemRepository;
            this.cartItemRepository = cartItemRepository;
            this.paymentRepository = paymentRepository;
            this.newebpayMerchantId = newebpayMerchantId;
        }

    // 立即購買：不經過購物車，直接用「一本書 × 數量」建立訂單。
    @Transactional
    public BookOrderCreateResponse createBookOrder(Long bookId, Long buyerMemberId, int quantity) {
        // 購買數量至少 1 本。
        if (quantity < 1) {
            throw new BookOrderException("quantity", ResponseMessages.ORDER_QUANTITY_INVALID);
        }

        // 先確認書籍存在且未被軟刪除；不存在就不要建立任何訂單或付款資料。
        Book book = findActiveBook(bookId);

        return createOrder(buyerMemberId, List.of(new OrderLine(book, quantity)));
    }

    // 購物車結帳：把購物車裡所有的書合成「一張訂單、一筆付款」，建立成功後清空購物車。
    @Transactional
    public BookOrderCreateResponse createOrderFromCart(Long buyerMemberId) {
        // 已依書籍 id 排序，扣庫存的順序固定，兩個人同時結帳時比較不會互相卡住。
        List<CartItem> cartItems = cartItemRepository.findByMemberIdOrderByBookIdAsc(buyerMemberId);
        if (cartItems.isEmpty()) {
            throw new BookOrderException("cart", ResponseMessages.CART_EMPTY);
        }

        List<OrderLine> lines = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            // 購物車裡有已下架的書就會在這裡丟出「找不到書籍」，整張訂單不會建立。
            lines.add(new OrderLine(findActiveBook(cartItem.getBookId()), cartItem.getQuantity()));
        }

        BookOrderCreateResponse response = createOrder(buyerMemberId, lines);

        // 訂單建立成功才清空購物車；上面任何一步失敗都會整個回復，購物車內容不會遺失。
        cartItemRepository.deleteByMemberId(buyerMemberId);
        return response;
    }

    // 建立訂單的共用流程（必須在交易內呼叫）：
    // 1. 逐本扣庫存（任何一本庫存不足就丟例外，前面已扣的庫存會跟著交易回復）。
    // 2. 建立訂單、訂單明細、付款單。
    private BookOrderCreateResponse createOrder(Long buyerMemberId, List<OrderLine> lines) {
        int totalQuantity = 0;
        int totalAmount = 0;

        for (OrderLine line : lines) {
            // 下單就先保留庫存：用單一 UPDATE 同時檢查並扣除，庫存不足就扣不到（受影響筆數 0）。
            // 付款失敗或逾時未付款時，庫存會再還回去。
            if (bookRepository.decreaseStock(line.book().getId(), line.quantity()) == 0) {
                throw new BookOrderException("book", ResponseMessages.BOOK_OUT_OF_STOCK);
            }
            totalQuantity += line.quantity();
            totalAmount += line.book().getPrice() * line.quantity();
        }

        // 產生訂單編號，格式：B + 年月日時分秒毫秒 + 4 碼亂數。
        String orderNo = generateOrderNo();

        // amount 是整張訂單的總金額；每本書的單價另外以快照存在訂單明細，
        // 避免日後 books.price 調整影響歷史訂單金額。
        BookOrder order = new BookOrder(orderNo, buyerMemberId, totalQuantity, totalAmount);
        bookOrderRepository.save(order);

        for (OrderLine line : lines) {
            Book book = line.book();
            bookOrderItemRepository.save(
                new BookOrderItem(order.getId(), book.getId(), book.getName(), book.getPrice(), line.quantity()));
        }

        // 新增付款單到資料庫
        Payment payment = new Payment(order.getId(), orderNo, newebpayMerchantId, order.getAmount());
        paymentRepository.save(payment);

        return new BookOrderCreateResponse(order, payment);
    }

    // 把這張訂單保留的庫存還回去（付款失敗、訂單逾時取消時使用）。
    // 會加入呼叫端的交易，所以和訂單狀態的修改會一起成功或一起回復。
    @Transactional
    public void restoreStock(Long orderId) {
        for (BookOrderItem item : bookOrderItemRepository.findByOrderId(orderId)) {
            bookRepository.increaseStock(item.getBookId(), item.getQuantity());
        }
    }

    // 產生給藍新的商品說明：一本書就用書名，多本書就是「第一本書名 等 N 項」，最多 50 字。
    public String buildItemDesc(Long orderId) {
        List<BookOrderItem> items = bookOrderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            return "書籍訂單";
        }

        String desc = items.get(0).getBookName();
        if (items.size() > 1) {
            desc = desc + " 等 " + items.size() + " 項";
        }
        return desc.length() > ITEM_DESC_MAX_LENGTH ? desc.substring(0, ITEM_DESC_MAX_LENGTH) : desc;
    }

    // 先確認書籍存在且未被軟刪除；不存在就不要建立任何訂單或付款資料。
    private Book findActiveBook(Long bookId) {
        try {
            return bookRepository.findOneById(bookId);
        } catch (NoResultException exception) {
            throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
        }
    }

    // 產生訂單編號，格式：B + 年月日時分秒毫秒 + 4 碼亂數。
    private String generateOrderNo() {
        DateTimeFormatter orderNoTimeFormat =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String orderNo = "";
        do {
            // 1000 ~ 9999 的亂數可以降低高併發下的碰撞機率。
            String random = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
            // 訂單編號格式：B + 年月日時分秒毫秒 + 4 碼亂數。
            // 例如 B202609081645301231234。
            orderNo = "B" + LocalDateTime.now().format(orderNoTimeFormat) + random;

            // 時間戳加亂數已經能大幅降低重複機率，但高併發下仍不是絕對不會碰撞。
            // 因此每次產生後都查一次 DB，確認 order_no 尚未存在；若已存在就重新產生。
        } while (bookOrderRepository.countByOrderNo(orderNo) > 0);

        return orderNo;
    }
}
