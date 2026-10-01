package charlie.gtalent_spring_boot_260801.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import charlie.gtalent_spring_boot_260801.entity.Book;
import charlie.gtalent_spring_boot_260801.entity.CartItem;
import charlie.gtalent_spring_boot_260801.exception.BookOrderException;
import charlie.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import charlie.gtalent_spring_boot_260801.repository.BookRepository;
import charlie.gtalent_spring_boot_260801.repository.CartItemRepository;
import charlie.gtalent_spring_boot_260801.response.CartItemResponse;
import charlie.gtalent_spring_boot_260801.response.CartResponse;
import jakarta.persistence.NoResultException;

// 購物車只是「想買的清單」，加入購物車不會扣庫存；庫存在結帳（建立訂單）時才扣。
@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;

    public CartService(CartItemRepository cartItemRepository, BookRepository bookRepository) {
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
    }

    // 取得購物車內容，每一筆都帶上書籍目前的名稱、價格、庫存。
    public CartResponse getCart(Long memberId) {
        List<CartItemResponse> items = new ArrayList<>();
        for (CartItem cartItem : cartItemRepository.findByMemberIdOrderByBookIdAsc(memberId)) {
            items.add(new CartItemResponse(cartItem, findBookOrNull(cartItem.getBookId())));
        }
        return new CartResponse(items);
    }

    // 加入購物車；同一本書已經在車裡就累加數量。累加後的數量不能超過目前庫存。
    @Transactional
    public CartResponse addItem(Long memberId, Long bookId, int quantity) {
        if (quantity < 1) {
            throw new BookOrderException("quantity", ResponseMessages.ORDER_QUANTITY_INVALID);
        }

        Book book = findActiveBook(bookId);
        CartItem cartItem = cartItemRepository.findByMemberIdAndBookId(memberId, bookId).orElse(null);

        int newQuantity = (cartItem == null ? 0 : cartItem.getQuantity()) + quantity;
        if (book.getStock() < newQuantity) {
            throw new BookOrderException("book", ResponseMessages.BOOK_OUT_OF_STOCK);
        }

        if (cartItem == null) {
            cartItem = new CartItem(memberId, bookId, newQuantity);
        } else {
            cartItem.setQuantity(newQuantity);
        }
        cartItemRepository.save(cartItem);

        return getCart(memberId);
    }

    // 直接把某一本書的數量改成指定值，不能超過目前庫存。
    @Transactional
    public CartResponse updateQuantity(Long memberId, Long bookId, int quantity) {
        if (quantity < 1) {
            throw new BookOrderException("quantity", ResponseMessages.ORDER_QUANTITY_INVALID);
        }

        CartItem cartItem = cartItemRepository.findByMemberIdAndBookId(memberId, bookId)
                .orElseThrow(() -> new ResourceNotFoundException("cart", ResponseMessages.CART_ITEM_NOT_FOUND));

        Book book = findActiveBook(bookId);
        if (book.getStock() < quantity) {
            throw new BookOrderException("book", ResponseMessages.BOOK_OUT_OF_STOCK);
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        return getCart(memberId);
    }

    // 從購物車移除一本書；本來就不在車裡也不會報錯。
    @Transactional
    public CartResponse removeItem(Long memberId, Long bookId) {
        cartItemRepository.deleteByMemberIdAndBookId(memberId, bookId);
        return getCart(memberId);
    }

    // 書籍存在且未被軟刪除就回傳；已刪除則回傳 null（購物車要能顯示「已下架」）。
    private Book findBookOrNull(Long bookId) {
        try {
            return bookRepository.findOneById(bookId);
        } catch (NoResultException exception) {
            return null;
        }
    }

    private Book findActiveBook(Long bookId) {
        Book book = findBookOrNull(bookId);
        if (book == null) {
            throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
        }
        return book;
    }
}
