package com.wcy.shorturl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wcy.shorturl.domain.po.TUrlMapping;
import com.wcy.shorturl.domain.po.User;
import com.wcy.shorturl.mapper.TUrlMappingMapper;
import com.wcy.shorturl.mapper.UserMapper;
import com.wcy.shorturl.service.ITUrlMappingService;
import com.wcy.shorturl.service.UserService;
import com.wcy.shorturl.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public String login(User login) {
        if (login == null || login.getUsername() == null || login.getPassword() == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        User one = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, login.getUsername())
                .eq(User::getPassword, login.getPassword()));

        if (one == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        Map<String ,Object> claims = new HashMap<>();
        claims.put("id",one.getId());
        String token=JwtUtil.gettoken(claims);
        return token;
    }
}
