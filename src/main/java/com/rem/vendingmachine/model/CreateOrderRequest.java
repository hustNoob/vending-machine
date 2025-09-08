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
    private List<Integer> productIds; // 商品 ID 列表
    private List<Integer> quantities; // 商品数量列表
}
