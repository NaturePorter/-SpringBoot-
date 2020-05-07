package cn.oncloud.service;

import cn.oncloud.dto.base.ResultConst;
import cn.oncloud.exception.BussinessException;
import cn.oncloud.mapper.UserMapper;
import cn.oncloud.pojo.User;
import cn.oncloud.util.JwtUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author 余弘洋
 * 描述：Service实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    UserService userService;

    @Autowired
    UserMapper userMapper;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private static String ISDELETE = "1";
    /**
     * 登录方法
     *
     * @param username 用户姓名
     * @param password 用户密码
     * @return 返回根据用户姓名和密码生成的token令牌
     */
    @Override
    public String login(String username, String password) {
        User user = null;
        try {
            LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>lambdaQuery().eq(User::getUsername, username).eq(User::getPassword, password);
            user = userService.getOne(queryWrapper);
            if (user == null) {
                throw new BussinessException(403, ResultConst.INVALID_PASSWORD);
            }
        } catch (Exception e) {
            throw new BussinessException(403, ResultConst.INVALID_PASSWORD);
        }

        try {
            if (ISDELETE.equals(user.getIsdelete())) {
                throw new BussinessException(403, ResultConst.USER_ISDELETE);
            }
            //生成jwt令牌
            String jwt = JwtUtil.createJWT(UUID.randomUUID().toString(), username, null);
            //将token和用户信息存储到redis中
            String json = JSON.toJSONString(user);
            stringRedisTemplate.opsForValue().set(jwt, json, JwtUtil.JWT_TTL, TimeUnit.MILLISECONDS);
            return jwt;
        }catch (Exception e) {
            throw new BussinessException(403, ResultConst.USER_ISDELETE);
        }
    }

    @Override
    public IPage<User> selectByUsername(Page<User> page, String userName) {
        return userMapper.selectByUsername(page, userName);
    }

    @Override
    public Integer updateUserStateById(Integer id, Integer state) {
        return userMapper.updateUserStateById(id, state);
    }

    @Override
    public void addUser(User user) {
        User user1 = new User();
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>lambdaQuery().eq(User::getUsername, user.getUsername());
        if (userService.getOne(queryWrapper) != null) {
            throw new BussinessException(HttpStatus.BAD_REQUEST.value(), ResultConst.USER_NAME_EXIST);
        }
        user1.setUsername(user.getUsername());
        user1.setPassword(user.getPassword());
        user1.setRole("用户");
        user1.setState("1");
        user1.setIsdelete("0");
        userService.save(user1);
    }

}