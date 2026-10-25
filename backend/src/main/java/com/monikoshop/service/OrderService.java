package com.monikoshop.service;



import java.util.Map;
import java.util.TreeMap;
import com.monikoshop.exception.ApiException;
import com.monikoshop.dto.*;
import com.monikoshop.entity.*;
import com.monikoshop.repository.OrderRepository;
import com.monikoshop.repository.ProductRepository;
import com.monikoshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    private static final int MAX_QUANTITY_PER_PRODUCT = 100;

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException("کاربر یافت نشد"));
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = currentUser();

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

            BigDecimal unitPrice = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
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
        order.setStatus(status);
        order = orderRepository.save(order);
        return toResponse(order, true);
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
                .build();
    }
}
