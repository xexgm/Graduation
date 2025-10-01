package com.gm.imbootstrap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gm.graduation.common.domain.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: Gemini
 * @date: 2025/9/29
 * des: 用户表的Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
