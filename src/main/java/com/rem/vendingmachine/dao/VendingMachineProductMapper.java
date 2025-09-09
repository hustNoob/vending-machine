package com.rem.vendingmachine.dao;

import com.rem.vendingmachine.model.VendingMachineProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VendingMachineProductMapper {

    // 插入一条商品记录到某台售货机（新增商品库存）
    int insertVendingMachineProduct(VendingMachineProduct vendingMachineProduct);

    // 删除某台售货机中的某商品记录
    int deleteVendingMachineProduct(@Param("vendingMachineId") int vendingMachineId,
                                    @Param("productId") int productId);

    // 更新某台售货机中某商品的库存
    int updateVendingMachineProductStock(@Param("vendingMachineId") int vendingMachineId,
                                         @Param("productId") int productId,
                                         @Param("stock") int stock,
                                         @Param("absStock") int absStock);

    // 查询某台售货机中的所有商品（库存信息）
    List<VendingMachineProduct> selectProductsByVendingMachineId(@Param("vendingMachineId") int vendingMachineId);

    // 查询某台售货机中是否存在某商品
    VendingMachineProduct selectVendingMachineProduct(@Param("vendingMachineId") int vendingMachineId, @Param("productId") int productId);

    int setVendingMachineProductStock(@Param("vendingMachineId") int vendingMachineId,
                                      @Param("productId") int productId,
                                      @Param("newStock") int newStock);
}