package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrdersMapper {

    /**
     * 用户下单
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /*
    根据user_id分页查询订单
     */
    Page<OrderVO> page(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    @Select("select * from orders where status = #{status}")
    List<Orders> getByStatus(Integer status);

    @Select("select * from orders where DATE(order_time) = #{date}")
    List<Orders> getByDate(LocalDate date);

    @Select("select * from orders where DATE(order_time) >= #{begin} and DATE(order_time) <= #{end} and status = 5")
    List<Orders> getByDateDur(LocalDate begin, LocalDate end);

    Integer countByMap(Map map);

    Double sumByMap(Map map);
}
