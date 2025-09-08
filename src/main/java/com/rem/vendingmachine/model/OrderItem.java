package com.rem.vendingmachine.model;

import java.math.BigDecimal;

public class OrderItem {
    private int id; // 主键
    private int orderId; // 订单ID
    private int productId; // 商品ID
    private int quantity; // 商品数量
    private BigDecimal price; // 商品单价
    private BigDecimal subtotal; // 小计金额

    public OrderItem() {}

    public OrderItem(int id, int orderId, int productId, int quantity, BigDecimal price, BigDecimal subtotal) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.subtotal = subtotal;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
