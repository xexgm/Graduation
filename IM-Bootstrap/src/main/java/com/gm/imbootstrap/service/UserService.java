package com.gm.imbootstrap.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gm.graduation.common.domain.User;
import com.gm.imbootstrap.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: Gemini
 * @date: 2025/9/29
 * des: 用户服务
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户注册
     *
     * @param userToRegister 包含用户名和密码的用户对象
     * @return 注册成功的用户对象
     * @throws Exception 如果用户名已存在
     */
    public User register(User userToRegister) throws Exception {
        // 1. 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", userToRegister.getUsername());
        if (userMapper.selectOne(queryWrapper) != null) {
            throw new Exception("用户名 '" + userToRegister.getUsername() + "' 已存在");
        }

        // 2. 在真实应用中，密码必须进行加密存储！
        // String hashedPassword = passwordEncoder.encode(userToRegister.getPassword());
        // userToRegister.setPassword(hashedPassword);

        // 3. 插入用户到数据库
        // userToRegister的id字段会在插入后由MyBatis-Plus自动回填（如果数据库ID是自增的）
        userMapper.insert(userToRegister);

        return userToRegister;
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录成功的用户对象
     * @throws Exception 如果用户不存在或密码错误
     */
    public User login(String username, String password) throws Exception {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);

        // 检查用户是否存在
        if (user == null) {
            throw new Exception("用户不存在");
        }

        // 检查密码是否匹配 (真实应用中应比较加密后的密码)
        if (!user.getPassword().equals(password)) {
            throw new Exception("密码错误");
        }

        // 登录成功，返回用户信息（通常不包含密码）
        return user;
    }

    /**
     * 用户登出
     *
     * @param userId 要登出的用户ID
     */
    public void logout(Long userId) {
        // 在无状态的REST服务中，登出通常由客户端删除token完成。
        // 如果使用session或在服务端维护了token状态，则在这里添加清除逻辑。
        System.out.println("用户 " + userId + " 已登出。");
    }
}
