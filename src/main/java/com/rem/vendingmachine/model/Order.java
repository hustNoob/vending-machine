package com.rem.vendingmachine.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Order {
    private int id; // 订单主键
    private int userId; // 用户ID
    private BigDecimal totalPrice; // 总金额
    private LocalDateTime createTime; // 下单时间
    private List<OrderItem> orderItems; // 商品清单
    private LocalDateTime paymentTime; // 支付时间
    private LocalDateTime completionTime; // 订单完成时间

    public boolean isPaid() {
        return paymentTime != null;
    }

    public boolean isCompleted() {
        return completionTime != null;
    }

    public LocalDateTime getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(LocalDateTime completionTime) {
        this.completionTime = completionTime;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public Order() {}

    public Order(int id, int userId, BigDecimal totalPrice, LocalDateTime createTime, List<OrderItem> orderItems) {
        this.id = id;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.createTime = createTime;
        this.orderItems = orderItems;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
