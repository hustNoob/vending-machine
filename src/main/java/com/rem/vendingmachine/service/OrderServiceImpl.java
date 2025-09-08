package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.OrderItemMapper;
import com.rem.vendingmachine.dao.OrderMapper;
import com.rem.vendingmachine.dao.ProductMapper;
import com.rem.vendingmachine.dao.UserMapper;
import com.rem.vendingmachine.model.Order;
import com.rem.vendingmachine.model.OrderItem;
import com.rem.vendingmachine.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    //根据账单，购物详细信息，创建一个账单
    @Override
    public boolean createOrder(Order order, List<Integer> productIds, List<Integer> quantities) {
        // 计算订单总金额
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            Integer productId = productIds.get(i);
            Integer quantity = quantities.get(i);

            // 查询商品详情
            Product product = productMapper.selectProductById(productId);

            if (product == null || product.getStock() < quantity) {
                throw new RuntimeException("Product is out of stock or does not exist: " + productId);
            }

            // 减少商品库存
            product.setStock(product.getStock() - quantity);
            productMapper.updateProduct(product);

            // 计算小计金额
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            // 创建订单子项
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(productId);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(product.getPrice());
            orderItem.setSubtotal(subtotal);
            orderItems.add(orderItem);

            // 累加到订单总金额
            totalPrice = totalPrice.add(subtotal);
        }

        order.setTotalPrice(totalPrice);

        // 插入订单主表
        orderMapper.insertOrder(order);

        // 插入子订单
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId()); // 设置主订单 ID
            orderItemMapper.insertOrderItem(orderItem);
        }

        return true;
    }

    //查询账单所有详细信息
    @Override
    public Order getOrderWithDetailsById(int orderId) {
        Order order = orderMapper.selectOrderById(orderId);

        if (order != null) {
            List<OrderItem> items = order.getOrderItems();
            if (items != null) { // 判空处理
                for (OrderItem item : items) {
                    // 查询并注入商品名称
                    String productName = productMapper.selectProductNameById(item.getProductId());
                    item.setProductName(productName);
                }
            } else {
                order.setOrderItems(new ArrayList<>()); // 如果为空，则初始化空列表
            }
        }
        return order;
    }


    //根据用户查询账单信息
    @Override
    public List<Order> getOrderHistoryByUserId(int userId) {
        return orderMapper.selectOrdersByUserId(userId);
    }

    //根据用户id推荐商品（3个）
    @Override
    public List<Integer> getRecommendedProductsForUser(int userId) {
        List<Integer> recommendedProducts = orderItemMapper.getTopPurchasedProductsByUser(userId);
        if (recommendedProducts.isEmpty()) {
            // 如果用户没有历史订单，推荐全局热销商品
            recommendedProducts = getTopSellingProducts();
        }
        return recommendedProducts;
    }

    //最热销商品
    @Override
    public List<Integer> getTopSellingProducts() {
        return orderItemMapper.getTopSellingProducts();
    }

    //支付账单
    @Override
    public boolean markOrderAsPaid(int orderId) {
        // 1. 查询订单详情
        Order order = orderMapper.selectOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found: " + orderId);
        }

        // 2. 查询用户余额
        BigDecimal userBalance = userMapper.getBalanceByUserId(order.getUserId());
        if (userBalance == null) {
            throw new RuntimeException("User balance not found for userId: " + order.getUserId());
        }

        // 3. 检查余额是否足够支付
        if (userBalance.compareTo(order.getTotalPrice()) < 0) {
            // 余额不足
            throw new RuntimeException("Insufficient balance. Current balance: " + userBalance);
        }

        // 4. 扣除余额
        BigDecimal newBalance = userBalance.subtract(order.getTotalPrice());
        userMapper.updateBalanceByUserId(order.getUserId(), newBalance);

        // 5. 更新订单支付状态
        int rowsAffected = orderMapper.updatePaymentStatus(orderId);
        if (rowsAffected == 0) {
            throw new RuntimeException("Order not found or already paid: " + orderId);
        }

        return true;
    }


    //完成账单
    @Override
    public boolean markOrderAsCompleted(int orderId) {
        int rowsAffected = orderMapper.updateCompletionStatus(orderId);
        if (rowsAffected == 0) {
            throw new RuntimeException("Order not found, not paid, or already completed: " + orderId);
        }
        return true;
    }

    //据账单号查询账单信息
    @Override
    public Order getOrderById(int orderId) {
        return orderMapper.selectOrderById(orderId);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderMapper.selectAllOrders();
    }

    @Override
    public void createOrderFromMachine(int vendingMachineId, int userId, String orderId, double totalPrice) {
    }

    public List<Order> queryOrders(Integer userId, Integer status, Integer machineId) {
        return orderMapper.queryOrders(userId, status, machineId);
    }
}