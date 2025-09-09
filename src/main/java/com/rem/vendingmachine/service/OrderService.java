package com.rem.vendingmachine.service;

import com.rem.vendingmachine.model.CheckoutRequest;
import com.rem.vendingmachine.model.Order;

import java.util.List;
import java.util.Map;

public interface OrderService {

    // 创建订单（购物车功能）
    boolean createOrder(int userId, List<CheckoutRequest.CartItem> cartItems);

    // 创建订单（支持直接手动指定的商品ID和数量）
    boolean createOrder(Order order, List<Integer> productIds, List<Integer> quantities);

    // 查询单个订单详情
    Order getOrderById(int orderId);

    // 查询订单详情（包含商品清单）
    Order getOrderWithDetailsById(int orderId);

    // 查询用户的历史订单
    List<Order> getOrderHistoryByUserId(int userId);

    // 推销
    List<Integer> getRecommendedProductsForUser(int userId);
    List<Integer> getTopSellingProducts();

    //订单支付
    boolean markOrderAsPaid(int orderId);

    //订单完成
    boolean markOrderAsCompleted(int orderId);

    //查询全部订单
    List<Order> getAllOrders();

    void createOrderFromMachine(int vendingMachineId, int userId, String orderId, double totalPrice);

    public List<Order> queryOrders(Integer userId, Integer status, Integer machineId);

    // 在OrderService接口中添加
    void processOrderFromMQTT(String orderId, int userId, int vendingMachineId, double totalPrice, List<Map<String, Object>> items);


}
