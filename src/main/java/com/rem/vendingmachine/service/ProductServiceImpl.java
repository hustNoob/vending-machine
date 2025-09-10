package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.OrderItemMapper;
import com.rem.vendingmachine.dao.OrderMapper;
import com.rem.vendingmachine.dao.ProductMapper;
import com.rem.vendingmachine.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public boolean addProduct(Product product) {
        return productMapper.insertProduct(product) > 0;
    }

    @Override
    public boolean updateProduct(Product product) {
        return productMapper.updateProduct(product) > 0;
    }

    @Override
    public boolean deleteProductById(int id) {
        // 检查商品是否被订单项引用
        if (orderItemMapper.countOrderItemsByProductId(id) > 0) {
            throw new RuntimeException("无法删除商品，该商品已被订单使用，不允许删除。");
        }
        // 执行删除操作
        return productMapper.deleteProductById(id) > 0;
    }

    @Override
    public List<Product> listAllProducts() {
        return productMapper.selectAllProduct();
    }

    @Override
    public Product getProductById(int id) {
        return productMapper.selectProductById(id);
    }

    @Override
    public List<Product> getRecommendedProducts(int userId) {
        List<Product> recommendedProducts = new ArrayList<>();

        // 1. 获取用户的历史订单商品，并按购买总量排序
        List<Integer> userTopProducts = orderItemMapper.getTopPurchasedProductsByUser(userId);

        // 2. 查询全局热销商品
        List<Integer> globalTopProducts = orderItemMapper.getTopSellingProducts();

        // 3. 根据用户购买记录优先推荐
        for (Integer productId : userTopProducts) {
            Product product = productMapper.selectProductById(productId);
            if (product != null) {
                recommendedProducts.add(product);
            }
        }

        // 4. 添加剩余的全局热销商品（排除已经推荐的用户购买商品）
        for (Integer productId : globalTopProducts) {
            if (userTopProducts.contains(productId)) continue; // 跳过已推荐商品
            Product product = productMapper.selectProductById(productId);
            if (product != null) {
                recommendedProducts.add(product);
            }
        }

        // 5. 如果系统没有任何销量记录，直接返回商品默认排序
        if (recommendedProducts.isEmpty()) {
            recommendedProducts = productMapper.selectAllProduct();
        }

        return recommendedProducts;
    }

    //下面两个暂时不用
    @Override
    public List<Product> getLowStockProducts() {
        return productMapper.selectLowStockProduct();
    }

    @Override
    public boolean restockProductById(int id, int quantity) {
        return productMapper.updateStock(id, quantity) > 0;
    }
}
