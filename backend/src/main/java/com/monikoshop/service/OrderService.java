package com.monikoshop.service;



import java.util.Map;
import java.util.TreeMap;
import com.monikoshop.audit.AuditEvent;
import com.monikoshop.audit.AuditService;
import com.monikoshop.exception.ApiException;
import com.monikoshop.security.CurrentUser;
import com.monikoshop.dto.*;
import com.monikoshop.entity.*;
import com.monikoshop.repository.OrderRepository;
import com.monikoshop.repository.ProductRepository;
import com.monikoshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private static final int MAX_QUANTITY_PER_PRODUCT = 100;

    /** سقف سفارش‌های «در انتظار» هر کاربر؛ جلوی خالی کردن موجودی با سفارش‌های پرداخت‌نشده را می‌گیرد. */
    @Value("${ORDER_MAX_PENDING:5}")
    private int maxPendingOrders = 5;

    private User currentUser() {
        Long id = CurrentUser.id();
        if (id == null) {
            throw new ApiException("کاربر یافت نشد");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException("کاربر یافت نشد"));
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = currentUser();

        if (orderRepository.countByUserIdAndStatus(user.getId(), OrderStatus.PENDING) >= maxPendingOrders) {
            auditService.log(AuditEvent.ORDER_BLOCKED, user.getId(), user.contactLabel(), "too many pending orders");
            throw new ApiException("شما " + maxPendingOrders
                    + " سفارش در انتظار دارید. لطفاً تا بررسی آن‌ها صبر کنید یا با پشتیبانی تماس بگیرید");
        }

        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .phone(request.getPhone())
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        // Merge duplicate lines and lock products in a fixed (id) order to avoid overselling and deadlocks.
        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderItemRequest itemRequest : request.getItems()) {
            quantities.merge(itemRequest.getProductId(), itemRequest.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            if (quantity < 1 || quantity > MAX_QUANTITY_PER_PRODUCT) {
                throw new ApiException("تعداد سفارش برای هر محصول باید بین ۱ و " + MAX_QUANTITY_PER_PRODUCT + " باشد");
            }

            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ApiException("محصول با شناسه " + productId + " یافت نشد"));

            if (!product.isActive()) {
                throw new ApiException("محصول \"" + product.getName() + "\" دیگر موجود نیست");
            }
            if (product.getStock() < quantity) {
                throw new ApiException("موجودی کافی برای \"" + product.getName() + "\" وجود ندارد");
            }

            BigDecimal unitPrice = effectiveUnitPrice(product);
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            total = total.add(subtotal);

            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
        }

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        auditService.log(AuditEvent.ORDER_CREATED, user.getId(), user.contactLabel(),
                "orderId=" + order.getId() + ", total=" + total);

        return toResponse(order, false);
    }

    public List<OrderResponse> getMyOrders() {
        User user = currentUser();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(o -> toResponse(o, false))
                .collect(Collectors.toList());
    }

    public OrderResponse getMyOrder(Long orderId) {
        User user = currentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("سفارش یافت نشد"));
        if (!order.getUser().getId().equals(user.getId())) {
            // same answer as "not found" so order ids cannot be probed
            throw ApiException.notFound("سفارش یافت نشد");
        }
        return toResponse(order, false);
    }

    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(o -> toResponse(o, true))
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("سفارش یافت نشد"));
        OrderStatus previous = order.getStatus();
        if (previous == OrderStatus.CANCELLED && status != OrderStatus.CANCELLED) {
            // موجودی این سفارش قبلاً به انبار برگشته؛ فعال‌کردن دوباره باعث ناهماهنگی انبار می‌شود
            throw new ApiException("سفارش لغوشده را نمی‌توان دوباره فعال کرد؛ یک سفارش جدید ثبت کنید");
        }
        if (previous != OrderStatus.CANCELLED && status == OrderStatus.CANCELLED) {
            restoreStock(order);
        }
        order.setStatus(status);
        order = orderRepository.save(order);
        auditService.log(AuditEvent.ADMIN_ORDER_STATUS, "orderId=" + orderId + ", " + previous + " -> " + status);
        return toResponse(order, true);
    }

    /**
     * قیمت واحد در لحظه‌ی خرید: قیمت با تخفیف فقط وقتی معتبر است که مثبت و کمتر از قیمت اصلی باشد
     * (تخفیف ۰ یا بیشتر از قیمت اصلی نباید باعث فروش رایگان یا گران‌تر شود).
     */
    static BigDecimal effectiveUnitPrice(Product product) {
        BigDecimal discount = product.getDiscountPrice();
        if (discount != null && discount.signum() > 0 && discount.compareTo(product.getPrice()) < 0) {
            return discount;
        }
        return product.getPrice();
    }

    /** با لغو سفارش، تعداد هر قلم به موجودی محصول برمی‌گردد (قفل محصولات به ترتیب id برای جلوگیری از deadlock). */
    private void restoreStock(Order order) {
        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() != null) {
                quantities.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            productRepository.findByIdForUpdate(entry.getKey()).ifPresent(product -> {
                product.setStock(product.getStock() + entry.getValue());
                productRepository.save(product);
            });
        }
    }

    private OrderResponse toResponse(Order order, boolean includeCustomer) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .subtotal(i.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .phone(order.getPhone())
                .createdAt(order.getCreatedAt())
                .items(items)
                .customerName(includeCustomer ? order.getUser().getFullName() : null)
                .customerEmail(includeCustomer ? order.getUser().getEmail() : null)
                .customerPhone(includeCustomer ? order.getUser().getPhone() : null)
                .build();
    }
}
