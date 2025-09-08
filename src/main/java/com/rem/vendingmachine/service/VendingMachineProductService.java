package com.rem.vendingmachine.service;

import com.rem.vendingmachine.model.VendingMachineProduct;

import java.math.BigDecimal;
import java.util.List;

public interface VendingMachineProductService {
    // 添加商品到售货机
    boolean addProductToMachine(int vendingMachineId, int productId, int stock, String productName, BigDecimal price);

    // 更新售货机商品的库存
    boolean updateProductStock(int vendingMachineId, int productId, int stock);

    // 查询某台售货机内所有商品及库存
    List<VendingMachineProduct> getProductsByMachineId(int vendingMachineId);

    // 从售货机中移除商品
    boolean removeProductFromMachine(int vendingMachineId, int productId);

    // 查询某台售货机中是否存在某商品
    VendingMachineProduct getProductInMachine(int vendingMachineId, int productId);
}