package com.wcy.shorturl.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wcy.shorturl.domain.po.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
