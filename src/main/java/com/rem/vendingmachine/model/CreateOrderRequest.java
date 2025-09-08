package com.rem.vendingmachine.model;

import java.util.List;

public class CreateOrderRequest {
    private int userId; // 用户 ID
    private List<Integer> productIds; // 商品 ID 列表
    private List<Integer> quantities; // 商品数量列表

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public List<Integer> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<Integer> productIds) {
        this.productIds = productIds;
    }

    public List<Integer> getQuantities() {
        return quantities;
    }

    public void setQuantities(List<Integer> quantities) {
        this.quantities = quantities;
    }
}
