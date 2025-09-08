package com.rem.vendingmachine.dao;

import com.rem.vendingmachine.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface UserMapper {

    int insertUser(User user);

    User selectUserByUsername(String username);

    User selectUserByUserId(int userId);

    List<User> selectAllUsers();

    //查询余额
    BigDecimal getBalanceByUserId(int userId);

    //更新余额
    int updateBalanceByUserId(@Param("userId") int userId, @Param("balance") BigDecimal balance);

    int updateUserByUserId(User user);

    int deleteUserByUserId(int userId);

}
