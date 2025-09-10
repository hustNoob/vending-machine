package com.rem.vendingmachine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {
    private int userId; // 用户 ID
    private String machineId; // 售货机 ID
    private List<CartItem> items; // 购物车中的商品列表

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CartItem {
        private int productId;       // 商品 ID
        private int vendingMachineId; // 售货机 ID（这个非常重要）
        private int quantity;        // 购买数量
    }
}

