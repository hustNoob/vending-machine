package com.rem.vendingmachine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendingMachineProduct {
    private int vendingMachineId;    // 售货机 ID
    private int productId;           // 商品 ID
    private String productName;      // 商品名称
    private BigDecimal price;        // 商品价格
    private int stock;               // 售货机内商品库存
}
