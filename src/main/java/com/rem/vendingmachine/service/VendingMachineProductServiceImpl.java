package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.VendingMachineProductMapper;
import com.rem.vendingmachine.model.VendingMachineProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class VendingMachineProductServiceImpl implements VendingMachineProductService {

    @Autowired
    private VendingMachineProductMapper vendingMachineProductMapper;

    @Override
    public boolean addProductToMachine(int vendingMachineId, int productId, int stock, String productName, BigDecimal price) {
        // 检查商品是否已经存在于该售货机中
        VendingMachineProduct existingProduct = vendingMachineProductMapper
                .selectVendingMachineProduct(vendingMachineId, productId);
        if (existingProduct != null) {
            // 如果商品已存在，则计算并设置新的总库存
            int newStock = existingProduct.getStock() + stock;
            return vendingMachineProductMapper.setVendingMachineProductStock(vendingMachineId, productId, newStock) > 0;
        }

        // 如果商品不存在于该售货机中，则插入一条新记录
        VendingMachineProduct newProduct = new VendingMachineProduct(vendingMachineId, productId, productName, price, stock);
        return vendingMachineProductMapper.insertVendingMachineProduct(newProduct) > 0;
    }

    @Override
    public boolean updateProductStock(int vendingMachineId, int productId, int stock) {
        // 库存更新 ---
        int absStock = Math.abs(stock);
        int rowsAffected = vendingMachineProductMapper.updateVendingMachineProductStock(
                vendingMachineId, productId, stock, absStock
        );
        return rowsAffected > 0;
    }

    @Override
    public List<VendingMachineProduct> getProductsByMachineId(int vendingMachineId) {
        return vendingMachineProductMapper.selectProductsByVendingMachineId(vendingMachineId);
    }

    @Override
    public boolean removeProductFromMachine(int vendingMachineId, int productId) {
        return vendingMachineProductMapper.deleteVendingMachineProduct(vendingMachineId, productId) > 0;
    }

    @Override
    public VendingMachineProduct getProductInMachine(int vendingMachineId, int productId) {
        return vendingMachineProductMapper.selectVendingMachineProduct(vendingMachineId, productId);
    }

    // 一个公开的设置绝对库存的方法 (供 Controller 调用)
    public boolean setProductStock(int vendingMachineId, int productId, int newStock) {
        return vendingMachineProductMapper.setVendingMachineProductStock(vendingMachineId, productId, newStock) > 0;
    }
}
