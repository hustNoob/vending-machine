package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.model.Product;
import com.rem.vendingmachine.service.OrderService;
import com.rem.vendingmachine.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    /**
     * 创建商品接口
     * @param product
     * @return 商品创建结果
     */
    @PostMapping("/add")
    public String addProduct(@RequestBody Product product) {
        if (productService.addProduct(product)) {
            return "商品添加成功！";
        }
        return "商品添加失败！";
    }

    /**
     * 更新商品接口
     * @param product 更新的商品
     * @return 商品更新结果
     */
    @PutMapping("/update")
    public String updateProduct(@RequestBody Product product) {
        if (productService.updateProduct(product)) {
            return "商品信息更新成功！";
        }
        return "商品信息更新失败！";
    }

    /**
     * 删除商品接口
     * @param id 删除商品的id
     * @return 商品删除结果
     */
    @DeleteMapping("/delete/{id}")
    public String deleteProduct(@PathVariable int id) {
        try {
            if (productService.deleteProductById(id)) {
                return "商品删除成功！";
            }
            return "商品删除失败！";
        } catch (RuntimeException e) {
            // 捕获业务异常并返回错误信息给前端
            return e.getMessage();
        }
    }

    /**
     * 查询所有商品接口
     * @return 商品查询结果
     */
    @GetMapping("/all")
    public List<Product> listAllProducts() {
        return productService.listAllProducts();
    }

    /**
     * 查询某商品接口
     * @param id 查询商品的id
     * @return 商品查询结果
     */
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable int id) {
        //return productService.getProductById(id);  原来的写法
        //学习另一种写法
        Product product = productService.getProductById(id);
        if (product == null) {
            throw new RuntimeException("id为"+id+"的商品未找到");
        }
        return product;
    }


    //下面二者暂时不用
    /**
     * 查询库存不足的商品接口
     * @return 库存不足的商品查询结果
     */
    @GetMapping("/low-stock")
    public List<Product> getLowStockProducts(){
        return productService.getLowStockProducts();
    }

    /**
     * 给目标商品补货接口
     * @param id 补货商品的id
     * @return 商品补货结果
     */
    @PutMapping("/restock/{id}")
    public String restockProduct(@PathVariable int id, @RequestParam int quantity){
        if (productService.restockProductById(id, quantity)) {
            return "补货成功！";
        }
        return "补货失败！";
    }

    /**
     * 给目标用户推荐商品接口
     * @param userId 目标用户的id
     * @return 推荐商品结果
     */
    @GetMapping("/recommend/{userId}")
    public List<Product> recommendProductsForUser(@PathVariable int userId) {
        return productService.getRecommendedProducts(userId);
    }
}
