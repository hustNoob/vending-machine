package com.rem.vendingmachine.controller;

import com.rem.vendingmachine.model.User;
import com.rem.vendingmachine.service.UserService;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 添加用户接口
     * @param user 用户信息
     * @return 添加用户结果
     */
    @PostMapping("/register")
    public String register(@RequestBody User user) {
        if(userService.registerUser(user)) {
            return "用户注册成功！";
        }
        return "该用户名已存在！";
    }

    /**
     * 登录用户接口
     * @param user 用户信息，对比用户名和密码
     * @return 登录用户结果
     */
    @PostMapping("/login")
    public String login(@RequestBody User user){
        User logger = userService.login(user.getUsername(), user.getPassword());
        if(logger != null) {
            return "登陆成功！";
        }
        return "登录失败，请重试！";
    }

    /**
     * 查询特定用户接口
     * @param userId 查询用户的id
     * @return 查询用户结果
     */
    @GetMapping("/id/{userId}")
    public User getUserByUserId(@PathVariable int userId) {
        return userService.getUserByUserId(userId);
    }

    /**
     * 查询特定用户接口
     * @param userName 查询用户的userName
     * @return 查询用户结果
     */
    @GetMapping("/username/{userName}")
    public User getUserByUserId(@PathVariable String userName) {
        return userService.getUserByUsername(userName);
    }

    /**
     * 查询所有用户接口
     * @return 查询所有用户结果
     */
    @GetMapping("/all")
    public List<User> getAllUsers(){
        return userService.getAllUsers();
    }

    @PutMapping("/update")
    public String updateUser(@RequestBody User user){
        if(userService.updateUser(user)) {
            return "用户更新成功";
        }
        return "用户更新失败";
    }

    @DeleteMapping("/delete/{id}")
    public String deleteUserById(@PathVariable int id){
        if(userService.deleteUserById(id)) {
            return "删除成功";
        }
        return "删除失败";
    }
}
