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
            // 如果商品已存在，则直接增加库存
            int newStock = existingProduct.getStock() + stock;
            return vendingMachineProductMapper.updateVendingMachineProductStock(vendingMachineId, productId, newStock) > 0;
        }

        // 如果商品不存在于该售货机中，则插入一条新记录
        VendingMachineProduct newProduct = new VendingMachineProduct(vendingMachineId, productId,productName, price,stock);
        return vendingMachineProductMapper.insertVendingMachineProduct(newProduct) > 0;
    }

    @Override
    public boolean updateProductStock(int vendingMachineId, int productId, int stock) {
        return vendingMachineProductMapper.updateVendingMachineProductStock(vendingMachineId, productId, stock) > 0;
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
}
