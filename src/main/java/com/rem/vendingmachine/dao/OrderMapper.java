package com.rem.vendingmachine.dao;

import com.rem.vendingmachine.model.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    int insertOrder(Order order); // 插入订单主信息

    Order selectOrderById(int id); // 查询订单详情

    List<Order> selectOrdersByUserId(@Param("userId") int userId); // 查询用户的订单记录

    int updatePaymentStatus(@Param("orderId") int orderId);

    int updateCompletionStatus(@Param("orderId") int orderId);

    List<Order> selectAllOrders();
}