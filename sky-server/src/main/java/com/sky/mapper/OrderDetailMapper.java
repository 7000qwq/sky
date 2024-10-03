package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {


    @Insert("insert into order_detail (setmeal_id, order_id, number, name, image, dish_id, dish_flavor, amount) " +
            "values (#{setmealId}, #{orderId}, #{number}, #{name}, #{image}, #{dishId}, #{dishFlavor}, #{amount})")
    void insert(OrderDetail orderDetail);

    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getByOrderId(Long id);
}
