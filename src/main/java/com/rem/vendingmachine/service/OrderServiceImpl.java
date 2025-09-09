package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.*;
import com.rem.vendingmachine.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Autowired
    private VendingMachineProductMapper vendingMachineProductMapper;

    @Override
    public boolean createOrder(int userId, List<CheckoutRequest.CartItem> cartItems) {
        // Step 1: 总金额计算
        BigDecimal totalPrice = BigDecimal.ZERO;

        // Step 2: 遍历购物车并校验库存
        for (CheckoutRequest.CartItem cartItem : cartItems) {
            // 从售货机中查询商品库存
            VendingMachineProduct vendingMachineProduct =
                    vendingMachineProductMapper.selectVendingMachineProduct(cartItem.getVendingMachineId(), cartItem.getProductId());

            // 如果商品不存在或库存不足，抛出异常
            if (vendingMachineProduct == null || vendingMachineProduct.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("库存不足，商品ID：" + cartItem.getProductId());
            }

            // 计算商品小计金额并累加到总金额
            BigDecimal subtotal = vendingMachineProduct.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalPrice = totalPrice.add(subtotal);
        }

        // Step 3: 检查用户余额是否充足
        BigDecimal userBalance = userMapper.getBalanceByUserId(userId);
        if (userBalance.compareTo(totalPrice) < 0) {
            throw new RuntimeException("余额不足！");
        }

        // Step 4: 创建订单并写入数据库
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalPrice(totalPrice);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insertOrder(order); // 假设这里成功

        // Step 5: 遍历购物车，插入订单项并扣减库存
        for (CheckoutRequest.CartItem cartItem : cartItems) {
            // 插入订单项
            VendingMachineProduct product = vendingMachineProductMapper.selectVendingMachineProduct(
                    cartItem.getVendingMachineId(),
                    cartItem.getProductId()
            );
            // 这里 price 和 subtotal 的计算可以复用 product.getPrice()
            BigDecimal itemPrice = product.getPrice();
            BigDecimal itemSubtotal = itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(itemPrice);
            orderItem.setSubtotal(itemSubtotal);
            orderItemMapper.insertOrderItem(orderItem);

            // --- 修改：调用更安全的库存更新方法 (扣减库存) ---
            int quantityToDeduct = -cartItem.getQuantity(); // 负数表示减少
            int absQuantity = Math.abs(quantityToDeduct);
            int rowsAffected = vendingMachineProductMapper.updateVendingMachineProductStock(
                    cartItem.getVendingMachineId(),
                    cartItem.getProductId(),
                    quantityToDeduct, // stock
                    absQuantity      // absStock
            );
            if (rowsAffected == 0) {
                // 更新失败，可能是超卖（并发）或记录不存在（理论上不应该，因为前面查过了）
                // 这里可以选择抛异常回滚事务，或者记录日志
                // 为了简化，我们抛异常
                throw new RuntimeException("扣减库存失败，商品ID: " + cartItem.getProductId() + "，可能是并发冲突或数据不一致，请重试。");
            }
            // --- 修改结束 ---
        }

        // Step 6: 扣减用户余额
        BigDecimal newBalance = userBalance.subtract(totalPrice);
        userMapper.updateBalanceByUserId(userId, newBalance);

        // Step 7: 更新订单支付状态 (这里可以考虑是否需要，因为余额已经扣了)
        // orderMapper.updatePaymentStatus(order.getId()); // 可选，取决于业务流程

        return true;
    }

    @Override
    public boolean createOrder(Order order, List<Integer> productIds, List<Integer> quantities) {
        // 总金额计算
        BigDecimal totalPrice = BigDecimal.ZERO;

        // 遍历商品，检查库存并计算总金额
        for (int i = 0; i < productIds.size(); i++) {
            int productId = productIds.get(i);
            int quantity = quantities.get(i);

            Product product = productMapper.selectProductById(productId);
            if (product == null) {
                throw new RuntimeException("商品不存在，商品ID: " + productId);
            }
            if (product.getStock() < quantity) {
                throw new RuntimeException("库存不足，商品ID: " + productId + "，库存：" + product.getStock());
            }

            // 计算小计并累加
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalPrice = totalPrice.add(subtotal);
        }

        // 检查用户余额是否充足（如果需要支付逻辑）
        BigDecimal userBalance = userMapper.getBalanceByUserId(order.getUserId());
        if (userBalance.compareTo(totalPrice) < 0) {
            throw new RuntimeException("余额不足，当前余额：" + userBalance);
        }

        // 减少库存并创建订单
        order.setTotalPrice(totalPrice);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insertOrder(order); // 主表插入

        // 插入订单项
        for (int i = 0; i < productIds.size(); i++) {
            int productId = productIds.get(i);
            int quantity = quantities.get(i);

            // 更新库存
            productMapper.updateStock(productId, -quantity);

            // 创建订单项
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(productId);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(productMapper.selectProductById(productId).getPrice());
            orderItem.setSubtotal(orderItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
            orderItemMapper.insertOrderItem(orderItem);
        }

        // 扣减用户余额
        BigDecimal newBalance = userBalance.subtract(totalPrice);
        userMapper.updateBalanceByUserId(order.getUserId(), newBalance);

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