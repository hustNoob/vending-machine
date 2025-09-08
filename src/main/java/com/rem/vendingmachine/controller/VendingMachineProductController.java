package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.model.Product;
import com.rem.vendingmachine.model.VendingMachineProduct;
import com.rem.vendingmachine.service.ProductService;
import com.rem.vendingmachine.service.VendingMachineProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vending-machine-product") // RESTful 接口前缀
public class VendingMachineProductController {

    @Autowired
    private VendingMachineProductService vendingMachineProductService;

    @Autowired
    private ProductService productService;

    /**
     * 获取某台售货机中的商品列表及其库存
     */
    @GetMapping("/{vendingMachineId}/products")
    public List<VendingMachineProduct> getProductsByMachineId(@PathVariable int vendingMachineId) {
        return vendingMachineProductService.getProductsByMachineId(vendingMachineId);
    }

    /**
     * 添加商品到售货机
     */
    /**
     * 添加商品到售货机
     */
    @PostMapping("/{vendingMachineId}/add-product")
    public String addProductToMachine(@PathVariable int vendingMachineId,
                                      @RequestParam int productId,
                                      @RequestParam int stock) {
        // 验证商品是否存在于厂商商品列表
        Product product = productService.getProductById(productId);
        if (product == null) {
            throw new RuntimeException("选择的商品不存在");
        }

        // 添加商品到售货机
        boolean success = vendingMachineProductService.addProductToMachine(
                vendingMachineId, productId, stock, product.getName(), product.getPrice()
        );

        return success ? "商品已成功添加到售货机！" : "添加商品失败，请重试！";
    }


    /**
     * 更新售货机中商品的库存
     */
    @PutMapping("/{vendingMachineId}/update-stock")
    public String updateProductStock(@PathVariable int vendingMachineId,
                                     @RequestParam int productId,
                                     @RequestParam int stock) {
        boolean success = vendingMachineProductService.updateProductStock(vendingMachineId, productId, stock);
        return success ? "库存更新成功！" : "库存更新失败，请重试！";
    }

    /**
     * 从售货机中移除商品
     */
    @DeleteMapping("/{vendingMachineId}/delete-product/{productId}")
    public String removeProductFromMachine(@PathVariable int vendingMachineId,
                                           @PathVariable int productId) {
        boolean success = vendingMachineProductService.removeProductFromMachine(vendingMachineId, productId);
        return success ? "商品已从售货机移除！" : "移除商品失败，请重试！";
    }
}