package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.*;
import com.rem.vendingmachine.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public boolean createOrder(Order order, List<CreateOrderRequest.CartItem> items) {
        // 总金额计算
        BigDecimal totalPrice = BigDecimal.ZERO;

        // 遍历商品，检查库存并计算总金额
        for (CreateOrderRequest.CartItem item : items) {
            int productId = item.getProductId();
            int vendingMachineId = item.getVendingMachineId(); // 关键：从item获取机器ID
            int quantity = item.getQuantity();

            // 🔥 关键修改：使用售货机商品表检查库存
            VendingMachineProduct vmProduct = vendingMachineProductMapper.selectVendingMachineProduct(
                    vendingMachineId, productId);

            if (vmProduct == null) {
                throw new RuntimeException("商品不存在于该售货机，商品ID: " + productId + ", 售货机ID: " + vendingMachineId);
            }
            if (vmProduct.getStock() < quantity) {
                throw new RuntimeException("库存不足，商品ID: " + productId + "，售货机库存：" + vmProduct.getStock());
            }

            // 计算小计并累加
            BigDecimal subtotal = vmProduct.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalPrice = totalPrice.add(subtotal);
        }

        // 检查用户余额是否充足
        BigDecimal userBalance = userMapper.getBalanceByUserId(order.getUserId());
        if (userBalance.compareTo(totalPrice) < 0) {
            throw new RuntimeException("余额不足，当前余额：" + userBalance);
        }

        // 减少库存并创建订单
        order.setTotalPrice(totalPrice);
        order.setCreateTime(LocalDateTime.now());
        orderMapper.insertOrder(order); // 主表插入

        // 插入订单项和扣减库存
        for (CreateOrderRequest.CartItem item : items) {
            int productId = item.getProductId();
            int vendingMachineId = item.getVendingMachineId();
            int quantity = item.getQuantity();

            // 1. 减少售货机库存
            vendingMachineProductMapper.updateVendingMachineProductStock(
                    vendingMachineId, productId, -quantity, quantity);

            // 2. 创建订单项
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(productId);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(vendingMachineProductMapper.selectVendingMachineProduct(vendingMachineId, productId).getPrice());
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
        System.out.println("【处理MQTT订单】订单ID: " + orderId + ", 售货机ID: " + vendingMachineId + ", 用户ID: " + userId + ", 金额: " + totalPrice);

        try {
            // 找到数据库中该售货机中的所有商品和库存
            List<VendingMachineProduct> products = vendingMachineProductMapper.selectProductsByVendingMachineId(vendingMachineId);

            System.out.println("【调试】售货机 " + vendingMachineId + " 中的商品列表: " + products);

            // 为了演示，我们假设订单数据存储在内存中的某个地方（实际应该从MQTT payload中获取）
            // 在实际系统中，你可能需要在MQTT消息中包含商品详情

            // 这里你可以添加完整的订单处理逻辑

            // 1. 检查用户余额
            BigDecimal userBalance = userMapper.getBalanceByUserId(userId);
            System.out.println("【调试】用户余额: " + userBalance + ", 订单金额: " + totalPrice);

            if (userBalance == null) {
                System.err.println("【错误】找不到用户余额");
                return;
            }

            BigDecimal totalAmount = BigDecimal.valueOf(totalPrice);
            if (userBalance.compareTo(totalAmount) < 0) {
                System.err.println("【错误】余额不足");
                return;
            }

            // 2. 扣减用户余额
            BigDecimal newBalance = userBalance.subtract(totalAmount);
            int rowsUpdated = userMapper.updateBalanceByUserId(userId, newBalance);
            System.out.println("【调试】用户余额更新结果: " + rowsUpdated);

            // 3. 创建订单（在数据库中）
            Order order = new Order();
            order.setUserId(userId);
            order.setTotalPrice(totalAmount);
            order.setCreateTime(LocalDateTime.now());
            orderMapper.insertOrder(order);
            System.out.println("【调试】订单创建成功，ID: " + order.getId());

            // 4. 如果有商品详情（这部分需要你重构或补充逻辑），就处理商品扣减
            // 由于MQTT消息结构，我们目前只能处理这些逻辑

            // 订单创建成功后，你可以在这里添加订单处理的其他业务逻辑
            System.out.println("【成功】订单处理完成: " + orderId);

        } catch (Exception e) {
            System.err.println("【错误】处理MQTT订单时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public List<Order> queryOrders(Integer userId, Integer status, Integer machineId) {
        return orderMapper.queryOrders(userId, status, machineId);
    }

    @Override
    public void processOrderFromMQTT(String tempOrderId, int userId, int vendingMachineId, double totalPrice, List<Map<String, Object>> items) {
        System.out.println("=== 处理完整的MQTT订单 ===");
        System.out.println("临时订单ID: " + tempOrderId + ", 用户ID: " + userId + ", 售货机ID: " + vendingMachineId);
        System.out.println("订单商品明细: " + items);

        try {
            // 1. 检查用户余额
            BigDecimal userBalance = userMapper.getBalanceByUserId(userId);
            if (userBalance == null) {
                System.err.println("【错误】用户余额查询失败");
                return;
            }

            BigDecimal totalAmount = BigDecimal.valueOf(totalPrice);
            if (userBalance.compareTo(totalAmount) < 0) {
                System.err.println("【错误】用户余额不足");
                return;
            }

            // 2. 检查库存并创建订单
            // 先验证所有商品库存是否充足
            for (Map<String, Object> item : items) {
                int productId = (Integer) item.get("productId");
                int quantity = (Integer) item.get("quantity");

                // 检查该售货机是否有该商品
                VendingMachineProduct vmProduct = vendingMachineProductMapper.selectVendingMachineProduct(vendingMachineId, productId);
                if (vmProduct == null) {
                    System.err.println("【错误】售货机 " + vendingMachineId + " 中不存在商品 " + productId);
                    return; // 直接返回，不处理订单
                }

                // 检查库存
                if (vmProduct.getStock() < quantity) {
                    System.err.println("【错误】商品 " + productId + " 库存不足，需要 " + quantity + "，现有 " + vmProduct.getStock());
                    return; // 直接返回，不处理订单
                }
            }

            // 3. 创建主订单 - 使用数据库自增ID
            Order order = new Order();
            order.setUserId(userId);
            order.setTotalPrice(totalAmount);
            order.setCreateTime(LocalDateTime.now());

            // 插入订单到数据库，获取真实的数据库ID
            orderMapper.insertOrder(order);
            int realOrderId = order.getId(); // 获取数据库生成的真实ID
            System.out.println("【调试】订单创建成功，真实数据库ID: " + realOrderId);

            // 4. 处理订单项和库存扣减
            for (Map<String, Object> item : items) {
                int productId = (Integer) item.get("productId");
                int quantity = (Integer) item.get("quantity");

                // 获取商品信息
                VendingMachineProduct vmProduct = vendingMachineProductMapper.selectVendingMachineProduct(vendingMachineId, productId);

                // 扣减库存
                vendingMachineProductMapper.updateVendingMachineProductStock(vendingMachineId, productId, -quantity, quantity);
                System.out.println("【调试】商品 " + productId + " 库存已扣减，减少 " + quantity);

                // 插入订单项
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(realOrderId); // 使用真实的数据库订单ID
                orderItem.setProductId(productId);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(vmProduct.getPrice());
                orderItem.setSubtotal(vmProduct.getPrice().multiply(BigDecimal.valueOf(quantity)));
                orderItemMapper.insertOrderItem(orderItem);
                System.out.println("【调试】订单项插入成功");
            }

            // 5. 更新用户余额
            BigDecimal newBalance = userBalance.subtract(totalAmount);
            userMapper.updateBalanceByUserId(userId, newBalance);
            System.out.println("【调试】用户余额已更新");

            System.out.println("【成功】MQTT订单处理完成，真实订单ID: " + realOrderId);

        } catch (Exception e) {
            System.err.println("【错误】处理MQTT订单时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 在 OrderServiceImpl.java 中添加实现
    @Override
    public List<Map<String, Object>> getTopSellingProductsWithQuantities() {
        return orderItemMapper.getTopSellingProductsWithQuantities();
    }

    // 添加辅助方法获取商品销量
    private int getProductSalesQuantity(int productId) {
        // 这里需要在 OrderItemMapper 中添加新的SQL查询
        // 暂时返回0，稍后实现
        return 0;
    }


}