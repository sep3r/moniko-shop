package com.monikoshop.service;

import com.monikoshop.audit.AuditService;
import com.monikoshop.dto.OrderItemRequest;
import com.monikoshop.dto.OrderRequest;
import com.monikoshop.dto.OrderResponse;
import com.monikoshop.entity.Order;
import com.monikoshop.entity.OrderItem;
import com.monikoshop.entity.OrderStatus;
import com.monikoshop.entity.Product;
import com.monikoshop.entity.User;
import com.monikoshop.exception.ApiException;
import com.monikoshop.repository.OrderRepository;
import com.monikoshop.repository.ProductRepository;
import com.monikoshop.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private AuditService auditService;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private OrderService service;

    @BeforeEach
    void login() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setFullName("Test User");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@test.com", "x", List.of()));
        lenient().when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    private static Product product(long id, String price, String discount, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName("محصول " + id);
        p.setPrice(new BigDecimal(price));
        p.setDiscountPrice(discount == null ? null : new BigDecimal(discount));
        p.setStock(stock);
        p.setActive(true);
        return p;
    }

    private static OrderRequest request(long productId, int quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        OrderRequest r = new OrderRequest();
        r.setItems(List.of(item));
        r.setShippingAddress("تهران");
        r.setPhone("09120000000");
        return r;
    }

    // ------------------------------------------------------------ pricing

    @Test
    void effectivePriceIgnoresZeroAndTooHighDiscounts() {
        assertEquals(new BigDecimal("100000"), OrderService.effectiveUnitPrice(product(1, "100000", null, 5)));
        assertEquals(new BigDecimal("80000"), OrderService.effectiveUnitPrice(product(1, "100000", "80000", 5)));
        assertEquals(new BigDecimal("100000"), OrderService.effectiveUnitPrice(product(1, "100000", "0", 5)));
        assertEquals(new BigDecimal("100000"), OrderService.effectiveUnitPrice(product(1, "100000", "150000", 5)));
    }

    @Test
    void orderUsesServerSidePriceAndReducesStock() {
        Product p = product(7, "100000", "80000", 10);
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(p));

        OrderResponse response = service.createOrder(request(7, 3));

        assertEquals(0, new BigDecimal("240000").compareTo(response.getTotalAmount()));
        assertEquals(7, p.getStock());
    }

    @Test
    void zeroDiscountDoesNotMakeTheProductFree() {
        Product p = product(7, "100000", "0", 10);
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(p));

        OrderResponse response = service.createOrder(request(7, 1));

        assertEquals(0, new BigDecimal("100000").compareTo(response.getTotalAmount()));
    }

    @Test
    void insufficientStockIsRejectedAndNothingIsSaved() {
        Product p = product(7, "100000", null, 2);
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(p));

        assertThrows(ApiException.class, () -> service.createOrder(request(7, 3)));
        assertEquals(2, p.getStock());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void tooManyPendingOrdersAreRejectedBeforeTouchingStock() {
        when(orderRepository.countByUserIdAndStatus(1L, OrderStatus.PENDING)).thenReturn(5L);

        assertThrows(ApiException.class, () -> service.createOrder(request(7, 1)));
        verify(productRepository, never()).findByIdForUpdate(7L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void inactiveProductCannotBeOrdered() {
        Product p = product(7, "100000", null, 5);
        p.setActive(false);
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(p));

        assertThrows(ApiException.class, () -> service.createOrder(request(7, 1)));
    }

    // ------------------------------------------------------ cancel / status

    private static Order orderWith(Product p, int quantity, OrderStatus status) {
        User user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("user@test.com");
        Order order = Order.builder().user(user).status(status).shippingAddress("تهران").phone("0912").build();
        order.addItem(OrderItem.builder().product(p).productName(p.getName())
                .unitPrice(p.getPrice()).quantity(quantity).subtotal(p.getPrice()).build());
        order.setTotalAmount(p.getPrice());
        return order;
    }

    @Test
    void cancellingAnOrderReturnsStockToTheShelf() {
        Product p = product(7, "100000", null, 5);
        Order order = orderWith(p, 3, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(p));

        service.updateStatus(10L, OrderStatus.CANCELLED);

        assertEquals(8, p.getStock());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void cancellingTwiceDoesNotReturnStockTwice() {
        Product p = product(7, "100000", null, 5);
        Order order = orderWith(p, 3, OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        service.updateStatus(10L, OrderStatus.CANCELLED);

        assertEquals(5, p.getStock());
        verify(productRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void cancelledOrderCannotBeReactivated() {
        Product p = product(7, "100000", null, 5);
        Order order = orderWith(p, 3, OrderStatus.CANCELLED);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(ApiException.class, () -> service.updateStatus(10L, OrderStatus.SHIPPED));
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void normalStatusChangeDoesNotTouchStock() {
        Product p = product(7, "100000", null, 5);
        Order order = orderWith(p, 3, OrderStatus.PENDING);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        service.updateStatus(10L, OrderStatus.CONFIRMED);

        assertEquals(5, p.getStock());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }
}
