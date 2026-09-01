package com.monikoshop.service;

import com.monikoshop.dto.*;
import com.monikoshop.entity.*;
import com.monikoshop.kafka.OrderEventProducer;
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
    private final OrderEventProducer orderEventProducer;

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("کاربر یافت نشد"));
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

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("محصول با شناسه " + itemRequest.getProductId() + " یافت نشد"));

            if (!product.isActive()) {
                throw new RuntimeException("محصول \"" + product.getName() + "\" دیگر موجود نیست");
            }
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new RuntimeException("موجودی کافی برای \"" + product.getName() + "\" وجود ندارد");
            }

            BigDecimal unitPrice = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(unitPrice)
                    .quantity(itemRequest.getQuantity())
                    .subtotal(subtotal)
                    .build();

            order.addItem(orderItem);
            total = total.add(subtotal);

            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);
        }

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        orderEventProducer.sendOrderCreatedEvent(order.getId(), user.getId(), order.getTotalAmount());

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
                .orElseThrow(() -> new RuntimeException("سفارش یافت نشد"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("دسترسی به این سفارش مجاز نیست");
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
                .orElseThrow(() -> new RuntimeException("سفارش یافت نشد"));
        order.setStatus(status);
        order = orderRepository.save(order);
        orderEventProducer.sendOrderStatusChangedEvent(order.getId(), status.name());
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
