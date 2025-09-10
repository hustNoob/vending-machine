package com.rem.vendingmachine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private int id; // 订单主键
    private int userId; // 用户ID
    private BigDecimal totalPrice; // 总金额
    private LocalDateTime createTime; // 下单时间
    private List<OrderItem> orderItems; // 商品清单
}
