package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.model.CreateOrderRequest;
import com.rem.vendingmachine.model.Order;
import com.rem.vendingmachine.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

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
            int userId = request.getUserId();
            List<Integer> productIds = request.getProductIds();
            List<Integer> quantities = request.getQuantities();

            Order order = new Order();
            order.setUserId(userId);
            order.setTotalPrice(BigDecimal.ZERO); // Service 会计算

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
}
