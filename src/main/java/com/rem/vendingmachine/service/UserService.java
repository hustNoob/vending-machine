package com.rem.vendingmachine.service;

import com.rem.vendingmachine.model.Product;
import com.rem.vendingmachine.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface UserService {
    boolean registerUser(User user);

    User login(String username, String password); //登录成功直接返回用户信息

    User getUserByUsername(String username);

    User getUserByUserId(int userId);

    List<User> getAllUsers();

    boolean updateUser(User user);

    boolean deleteUserById(int userId);

    List<Product> getRecommendedProducts(int userId);

    boolean updateUserBalance(int userId, BigDecimal balance);
}
