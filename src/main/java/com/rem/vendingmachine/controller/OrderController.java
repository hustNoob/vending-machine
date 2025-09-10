package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.model.CheckoutRequest;
import com.rem.vendingmachine.model.CreateOrderRequest;
import com.rem.vendingmachine.model.Order;
import com.rem.vendingmachine.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单接口
     * @param request 订单请求体，把创建订单封装成一个专门的类
     * @return 订单创建结果
     */
    // 修改 OrderController 中的 createOrder
    @PostMapping("/create")
    public String createOrder(@RequestBody CreateOrderRequest request) {
        try {
            int userId = request.getUserId();
            String machineIdStr = request.getMachineId();
            List<CreateOrderRequest.CartItem> items = request.getItems();

            // 如果需要，可以在这里验证机器ID
            int machineId = Integer.parseInt(machineIdStr);

            // 如果你还需要从items中构造 productIds 和 quantities
            List<Integer> productIds = items.stream().map(CreateOrderRequest.CartItem::getProductId).collect(Collectors.toList());
            List<Integer> quantities = items.stream().map(CreateOrderRequest.CartItem::getQuantity).collect(Collectors.toList());

            Order order = new Order();
            order.setUserId(userId);

            // 调用顺序修改
            boolean success = orderService.createOrder(order, items); // 传入items更好

            if (success) {
                return "订单创建成功！订单ID: " + order.getId();
            } else {
                return "订单创建失败！";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 查询某订单详情接口
     * @param orderId 订单 ID
     * @return 订单详情
     */
    @GetMapping("/details/{orderId}")
    public Order getOrderWithDetailsById(@PathVariable int orderId) {
        return orderService.getOrderWithDetailsById(orderId);
    }

    /**
     * 查询某用户历史订单接口
     * @param userId 用户 ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    public List<Order> getOrderHistoryByUserId(@PathVariable int userId) {
        return orderService.getOrderHistoryByUserId(userId);
    }

    /**
     * 查询某订单信息
     * @param orderId 订单 ID
     * @return 订单信息
     */
    @GetMapping("/{orderId}")
    public Order getOrderById(@PathVariable int orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            throw new RuntimeException("订单未找到: " + orderId);
        }
        // 移除支付状态和完成状态的输出
        // 系统不再有支付/完成状态
        return order;
    }

    @GetMapping("/all")
    public List<Order> getAllOrders(){
        return orderService.getAllOrders();
    }

    @GetMapping("/query")
    public List<Order> queryOrders(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer machineId) {
        return orderService.queryOrders(userId, status, machineId);
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request) {
        try {
            boolean success = orderService.createOrder(request.getUserId(), request.getOrder());
            if (!success) {
                throw new RuntimeException("支付失败，请检查余额或库存");
            }
            return ResponseEntity.ok("支付成功！");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 添加一个新接口，允许后端直接通过API创建订单（而不是用户页面交互）
    @PostMapping("/create-by-mqtt")
    public String createOrderFromMQTT(@RequestBody Map<String, Object> orderData) {
        try {
            int userId = (Integer) orderData.get("userId");
            String machineId = (String) orderData.get("machineId");
            String orderId = (String) orderData.get("orderId");
            double totalPrice = ((Number) orderData.get("totalPrice")).doubleValue();

            System.out.println("通过API创建订单 - 订单ID: " + orderId);

            return "订单成功处理: " + orderId;
        } catch (Exception e) {
            return "订单创建失败: " + e.getMessage();
        }
    }

    // 在OrderController中添加这个方法
    @PostMapping("/mqtt-order")
    public String handleMQTTOrder(@RequestBody Map<String, Object> orderData) {
        try {
            String orderId = (String) orderData.get("orderId");
            int userId = (Integer) orderData.get("userId");
            String machineIdStr = (String) orderData.get("machineId");
            double totalPrice = ((Number) orderData.get("totalPrice")).doubleValue();

            List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");

            int machineId = Integer.parseInt(machineIdStr);

            orderService.processOrderFromMQTT(orderId, userId, machineId, totalPrice, items);

            return "订单处理成功: " + orderId;
        } catch (Exception e) {
            System.err.println("处理MQTT订单时出错: " + e.getMessage());
            return "订单处理失败: " + e.getMessage();
        }
    }

    @GetMapping("/top-selling")
    public List<Integer> getTopSellingProducts() {
        return orderService.getTopSellingProducts();
    }

    @GetMapping("/top-selling-with-quantities")
    public List<Map<String, Object>> getTopSellingProductsWithQuantities() {
        return orderService.getTopSellingProductsWithQuantities();
    }
}