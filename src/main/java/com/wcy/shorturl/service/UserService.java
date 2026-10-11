package com.wcy.shorturl.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wcy.shorturl.domain.po.User;

public interface UserService extends IService<User> {
    String login(User login);
}
