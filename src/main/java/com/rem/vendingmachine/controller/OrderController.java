package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.model.CheckoutRequest;
import com.rem.vendingmachine.model.CreateOrderRequest;
import com.rem.vendingmachine.model.Order;
import com.rem.vendingmachine.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
    @PostMapping("/create")
    public String createOrder(@RequestBody CreateOrderRequest request) {
        try {
            // 获取请求参数
            int userId = request.getUserId();
            List<Integer> productIds = request.getProductIds();
            List<Integer> quantities = request.getQuantities();

            Order order = new Order();
            order.setUserId(userId);

            // 调用 createOrder（共享逻辑）
            boolean success = orderService.createOrder(order, productIds, quantities);

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
     * 模拟订单支付操作
     * @param orderId 订单 ID
     * @return 支付完成提示
     */
    @PutMapping("/pay/{orderId}")
    public String payOrder(@PathVariable int orderId) {
        try {
            orderService.markOrderAsPaid(orderId);
            return "订单支付成功！";
        } catch (Exception e) {
            return "订单支付失败: " + e.getMessage();
        }
    }

    /**
     * 模拟订单完成操作
     * @param orderId 订单 ID
     * @return 订单完成提示
     */
    @PutMapping("/complete/{orderId}")
    public String completeOrder(@PathVariable int orderId) {
        try {
            orderService.markOrderAsCompleted(orderId);
            return "订单已完成!";
        } catch (Exception e) {
            return "订单出现问题: " + e.getMessage();
        }
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
        System.out.println("订单支付情况: " + order.isPaid());
        System.out.println("订单完成情况: " + order.isCompleted());
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

            // 这里其实应该调用 orderService.createOrderFromMachine 来处理逻辑
            // 但为了简化，你可以在这里添加实际的订单处理逻辑

            // 可选：在这里处理订单创建逻辑，比如扣减库存等
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

}
