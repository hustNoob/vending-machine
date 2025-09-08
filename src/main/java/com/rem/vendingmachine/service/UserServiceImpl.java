package com.rem.vendingmachine.service;

import com.rem.vendingmachine.dao.UserMapper;
import com.rem.vendingmachine.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

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
}
