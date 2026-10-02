package charlie.gtalent_spring_boot_260801.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import charlie.gtalent_spring_boot_260801.constant.OrderStatus;
import charlie.gtalent_spring_boot_260801.constant.ResponseMessages;
import charlie.gtalent_spring_boot_260801.entity.BookOrder;
import charlie.gtalent_spring_boot_260801.entity.BookOrderItem;
import charlie.gtalent_spring_boot_260801.exception.BookOrderException;
import charlie.gtalent_spring_boot_260801.repository.BookOrderItemRepository;
import charlie.gtalent_spring_boot_260801.repository.BookOrderQueryRepository;
import charlie.gtalent_spring_boot_260801.response.OrderItemResponse;
import charlie.gtalent_spring_boot_260801.response.OrderResponse;
import charlie.gtalent_spring_boot_260801.response.PageResponse;
import jakarta.persistence.criteria.Predicate;

// 會員查詢自己的購買紀錄。
@Service
public class OrderQueryService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.CANCELLED, OrderStatus.FAILED);

    private final BookOrderQueryRepository bookOrderQueryRepository;
    private final BookOrderItemRepository bookOrderItemRepository;

    public OrderQueryService(
            BookOrderQueryRepository bookOrderQueryRepository,
            BookOrderItemRepository bookOrderItemRepository) {
        this.bookOrderQueryRepository = bookOrderQueryRepository;
        this.bookOrderItemRepository = bookOrderItemRepository;
    }

    // 查詢條件都是選填；memberId 一定會加進條件，所以只會查到自己的訂單。
    // from / to 是日期範圍（含當天），依訂單建立時間篩選。
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> search(
            Long memberId, int page, int size,
            String status, String orderNo, LocalDate from, LocalDate to) {

        String statusFilter = (status == null || status.isBlank()) ? null : status.trim();
        if (statusFilter != null && !ALLOWED_STATUSES.contains(statusFilter)) {
            throw new BookOrderException("status", ResponseMessages.VALIDATION_FAILED);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BookOrderException("date", ResponseMessages.VALIDATION_FAILED);
        }
        String orderNoFilter = (orderNo == null || orderNo.isBlank()) ? null : orderNo.trim();

        Specification<BookOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("buyerMemberId"), memberId));
            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("orderStatus"), statusFilter));
            }
            if (orderNoFilter != null) {
                // 把使用者輸入的 % 和 _ 當成一般文字，不當成萬用字元。
                String escaped = orderNoFilter.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(root.get("orderNo"), "%" + escaped + "%", '\\'));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                // to 含當天，所以用「小於隔天 00:00」。
                predicates.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        Page<BookOrder> result = bookOrderQueryRepository.findAll(spec, PageRequest.of(page - 1, size, sort));

        // 一次查出這一頁所有訂單的明細，再依訂單分組。
        List<Long> orderIds = result.getContent().stream().map(BookOrder::getId).toList();
        Map<Long, List<OrderItemResponse>> itemsByOrder = orderIds.isEmpty()
                ? Map.of()
                : bookOrderItemRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(
                                BookOrderItem::getOrderId,
                                Collectors.mapping(OrderItemResponse::new, Collectors.toList())));

        List<OrderResponse> responses = result.getContent().stream()
                .map(order -> new OrderResponse(order, itemsByOrder.getOrDefault(order.getId(), List.of())))
                .toList();

        return new PageResponse<>(responses, page, size, result.getTotalElements());
    }
}
