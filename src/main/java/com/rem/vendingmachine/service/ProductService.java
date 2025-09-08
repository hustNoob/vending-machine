package com.rem.vendingmachine.service;

import com.rem.vendingmachine.model.Product;

import java.util.List;

public interface ProductService {

    boolean addProduct(Product product); //加商品

    boolean updateProduct(Product product); //改商品信息

    boolean deleteProductById(int id); //按id删除商品

    List<Product> listAllProducts(); //所有商品

    Product getProductById(int id); //据id查商品


    List<Product> getLowStockProducts(); //查库存不足的商品
    boolean restockProductById(int id, int quantity);//补库存
}
