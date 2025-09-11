package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.*;
import com.rem.vendingmachine.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

        // 不再需要手动设置商品名称，因为已经在SQL查询中关联了

        if (order != null && order.getOrderItems() == null) {
            order.setOrderItems(new ArrayList<>()); // 如果为空，则初始化空列表
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

    //据账单号查询账单信息
    @Override
    public Order getOrderById(int orderId) {
        return orderMapper.selectOrderById(orderId);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderMapper.selectAllOrders();
    }


    public List<Order> queryOrders(Integer userId, Integer status, Integer machineId) {
        return orderMapper.queryOrders(userId, status, machineId);
    }

    // ---
    // 由于 handleOrder 已经直接调用了 processOrderCore，
    // 并且提供了所需的所有参数（items），
    // processOrderFromMQTT 方法变得多余，可以标记为 @Deprecated，或者重定向调用。
    // 保留它是为了兼容可能从 Controller (如 OrderController.handleMQTTOrder) 调用的情况。
    // ---

    @Override
    public boolean processOrderFromMQTT(String tempOrderId, int userId, int vendingMachineId, double totalPriceIgnored, List<Map<String, Object>> items) {
        System.out.println("=== OrderServiceImpl.processOrderFromMQTT 开始处理【完整】订单 ===");
        System.out.println("临时订单ID: " + tempOrderId + ", 用户ID: " + userId + ", 售货机ID: " + vendingMachineId);
        System.out.println("订单商品明细: " + items);

        // --- 关键修改：这个方法现在不再依赖 totalPrice 和 tempOrderId 来获取信息 ---
        // 它将完全信任 items 列表，并据此执行所有操作。

        try {
            // 1. 检查用户余额
            BigDecimal userBalance = userMapper.getBalanceByUserId(userId);
            if (userBalance == null) {
                System.err.println("【服务端错误】找不到用户 (ID: " + userId + ") 或查询余额失败。");
                return false; // 返回 false 表示处理失败
            }

            // 2. 初始化计算总价
            BigDecimal calculatedTotal = BigDecimal.ZERO;

            // 3. 预先验证所有商品库存和计算总价
            //    为避免在循环中多次查询数据库，可以先查询一次相关信息
            List<VendingMachineProduct> vmProducts = new ArrayList<>();
            List<BigDecimal> itemSubtotals = new ArrayList<>();

            for (Map<String, Object> item : items) {
                int productId = (Integer) item.get("productId");
                int quantity = (Integer) item.get("quantity");

                // a. 查询售货机商品信息 (价格、当前库存)
                VendingMachineProduct vmProduct = vendingMachineProductMapper.selectVendingMachineProduct(vendingMachineId, productId);
                if (vmProduct == null) {
                    System.err.println("【服务端错误】售货机 " + vendingMachineId + " 中不存在商品 " + productId);
                    return false; // 返回 false 表示处理失败
                }

                // b. 检查库存是否足够
                if (vmProduct.getStock() < quantity) {
                    System.err.println("【服务端错误】商品 " + productId + " 库存不足。需要: " + quantity + ", 现有: " + vmProduct.getStock());
                    return false; // 返回 false 表示处理失败
                }

                // c. 计算小计 (务必使用数据库中的价格)
                BigDecimal itemPrice = vmProduct.getPrice();
                BigDecimal itemSubtotal = itemPrice.multiply(BigDecimal.valueOf(quantity));
                calculatedTotal = calculatedTotal.add(itemSubtotal);

                // d. 缓存查询和计算结果，供后续使用
                vmProducts.add(vmProduct);
                itemSubtotals.add(itemSubtotal);
            }

            // 4. 检查总金额是否超过用户余额
            if (userBalance.compareTo(calculatedTotal) < 0) {
                System.err.println("【服务端错误】用户 (ID: " + userId + ") 余额不足。余额: " + userBalance + ", 订单总价: " + calculatedTotal);
                return false; // 返回 false 表示处理失败
            }

            // 5. --- 关键操作：所有校验通过，开始数据库操作 (应在事务中) ---

            // a. 创建主订单 (使用计算出的总价)
            Order order = new Order();
            order.setUserId(userId);
            order.setTotalPrice(calculatedTotal); // 使用服务端计算的总价
            order.setCreateTime(LocalDateTime.now());

            orderMapper.insertOrder(order); // 插入订单，获取数据库真实ID
            int realOrderId = order.getId(); // 获取真实ID
            System.out.println("【服务端调试】订单创建成功，真实数据库ID: " + realOrderId);

            // b. 处理订单项和库存扣减
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                VendingMachineProduct vmProduct = vmProducts.get(i); // 使用预查询结果
                BigDecimal itemSubtotal = itemSubtotals.get(i);      // 使用预计算小计

                int productId = (Integer) item.get("productId");
                int quantity = (Integer) item.get("quantity");

                // i. 扣减售货机库存 (传负数)
                vendingMachineProductMapper.updateVendingMachineProductStock(vendingMachineId, productId, -quantity, quantity);
                System.out.println("【服务端调试】商品 " + productId + " 库存已扣减 " + quantity);

                // ii. 插入订单项
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(realOrderId); // 使用真实的数据库订单ID
                orderItem.setProductId(productId);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(vmProduct.getPrice()); // 使用数据库价格
                orderItem.setSubtotal(itemSubtotal);      // 使用预计算小计
                orderItemMapper.insertOrderItem(orderItem);
                System.out.println("【服务端调试】订单项已插入，商品ID: " + productId + ", 数量: " + quantity);
            }

            // c. 更新用户余额
            BigDecimal newBalance = userBalance.subtract(calculatedTotal);
            userMapper.updateBalanceByUserId(userId, newBalance);
            System.out.println("【服务端调试】用户 (ID: " + userId + ") 余额已更新为: " + newBalance);

            // 6. --- 操作完成 ---
            System.out.println("【服务端成功】MQTT订单 (ID: " + tempOrderId + " -> DB ID: " + realOrderId + ") 处理完成。");
            return true; // 返回 true 表示处理成功

        } catch (Exception e) {
            // 7. --- 异常处理 ---
            System.err.println("【服务端严重错误】处理MQTT订单时发生未预期异常: " + e.getMessage());
            e.printStackTrace();
            // Spring 的 @Transactional 会自动回滚事务
            // 如果手动管理事务，这里需要回滚
            return false; // 返回 false 表示处理失败
        } finally {
            System.out.println("=== OrderServiceImpl.processOrderFromMQTT 处理流程结束 ===");
        }
    }

    @Override
    public List<Map<String, Object>> getTopSellingProductsWithQuantities() {
        return orderItemMapper.getTopSellingProductsWithQuantities();
    }
}