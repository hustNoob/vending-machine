package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.ProductMapper;
import com.rem.vendingmachine.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

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
    public List<Product> getLowStockProducts() {
        return productMapper.selectLowStockProduct();
    }

    @Override
    public boolean restockProductById(int id, int quantity) {
        return productMapper.updateStock(id, quantity) > 0;
    }
}
