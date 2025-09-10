package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.OrderItemMapper;
import com.rem.vendingmachine.dao.ProductMapper;
import com.rem.vendingmachine.dao.UserMapper;
import com.rem.vendingmachine.model.Product;
import com.rem.vendingmachine.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public boolean registerUser(User user) {
        if(userMapper.selectUserByUsername(user.getUsername())!=null){ //已经有人了
            return false;
        }
        userMapper.insertUser(user);
        return true;
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.selectUserByUsername(username);
        if(user!=null && user.getPassword().equals(password)){
            return user;
        }
        return null;
    }

    @Override
    public User getUserByUserId(int userId) {
        return userMapper.selectUserByUserId(userId);
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectUserByUsername(username);
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.selectAllUsers();
    }

    @Override
    public boolean updateUser(User user) {
        return userMapper.updateUserByUserId(user)>0;
    }

    @Override
    public boolean deleteUserById(int userId) {
        return userMapper.deleteUserByUserId(userId)>0;
    }

    @Override
    public List<Product> getRecommendedProducts(int userId) {
        // 1. 获取用户的历史订单商品，并按购买总量排序
        List<Integer> userTopProducts = orderItemMapper.getTopPurchasedProductsByUser(userId);

        // 2. 查询全局热销商品
        List<Integer> globalTopProducts = orderItemMapper.getTopSellingProducts();

        // 3. 构建推荐商品列表
        List<Product> recommendedProducts = new ArrayList<>();
        Set<Integer> addedProductIds = new HashSet<>();

        // 3.1 根据用户购买记录优先推荐
        for (Integer productId : userTopProducts) {
            if (!addedProductIds.contains(productId)) {
                Product product = productMapper.selectProductById(productId);
                if (product != null) {
                    recommendedProducts.add(product);
                    addedProductIds.add(productId);
                }
            }
        }

        // 3.2 添加剩余的全局热销商品（排除已经推荐的用户购买商品）
        for (Integer productId : globalTopProducts) {
            if (!addedProductIds.contains(productId)) {
                Product product = productMapper.selectProductById(productId);
                if (product != null) {
                    recommendedProducts.add(product);
                    addedProductIds.add(productId);
                }
            }
        }

        // 3.3 如果系统没有任何销量记录，直接返回商品默认排序
        if (recommendedProducts.isEmpty()) {
            recommendedProducts = productMapper.selectAllProduct();
        }

        return recommendedProducts;
    }

}
