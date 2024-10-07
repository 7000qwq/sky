package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    @Select("select * from user where DATE(create_time) = #{date}")
    List<User> getByDate(LocalDate date);

    @Select("select COUNT(*) from user where DATE(create_time) < #{beginDate}")
    int getByBeforeDate(LocalDate beginDate);

    @Select("select COUNT(*) from user where create_time <= #{begin} and create_time >= #{end}")
    Integer countByMap(Map map);
}
