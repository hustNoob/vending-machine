package com.rem.vendingmachine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    private int id; // 主键
    private int orderId; // 订单ID
    private int productId; // 商品ID
    private int quantity; // 商品数量
    private BigDecimal price; // 商品单价
    private BigDecimal subtotal; // 小计金额
    private String productName; // 商品名称
}
