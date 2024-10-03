package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /**
     * 根据openid获取user
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 根据id获取username
     * @param id
     * @return
     */
    @Select("select name from user where id = #{id}")
    String getNameById(Long id);

    /**
     * 自动注册
     * @param user
     */
    void insert(User user);

    @Select("select * from user where id = #{userId}")
    User getById(Long userId);
}
