package com.rem.vendingmachine.dao;

import com.rem.vendingmachine.model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    int insertProduct(Product product);
    int deleteProductById(@Param("id") int id);
    int updateProduct(Product product);
    List<Product> selectAllProduct();
    Product selectProductById(@Param("id") int id);
    List<Product> selectLowStockProduct();
    int updateStock(@Param("id") int id, @Param("quantity") int quantity); //根据库存不足的id去添加库存
}
