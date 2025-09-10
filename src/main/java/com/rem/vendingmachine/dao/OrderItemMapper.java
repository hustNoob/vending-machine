package com.rem.vendingmachine.dao;

import com.rem.vendingmachine.model.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderItemMapper {
    int insertOrderItem(OrderItem orderItem); // 插入子订单
    List<OrderItem> selectOrderItemsByOrderId(@Param("orderId") int orderId); // 查询订单的商品清单
    List<Integer> getTopPurchasedProductsByUser(@Param("userId")int userId); //查询用户购买数量最多的商品的id
    List<Integer> getTopSellingProducts(); //查询系统热销商品的id

    List<Map<String, Object>> getTopSellingProductsWithQuantities();
}
