package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class SchedulTask {

    @Autowired
    private OrdersMapper ordersMapper;

    /*
    @Scheduled(cron = "0/5 * * * * ?")
    public void Task(){
        log.info("过了五秒: {}", new Date());
    }
    */

    @Scheduled(cron = "0 * * * * ?")
    public void payTimeout(){
        log.info("过了1分钟，检查是否需要取消超时订单");
        List<Orders> ordersList = ordersMapper.getByStatus(Orders.PENDING_PAYMENT);
        for (Orders orders : ordersList) {
            if (Duration.between(orders.getOrderTime(), LocalDateTime.now()).toMinutes() > 15){
                //超过15分钟需要取消了
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单支付超时");
                orders.setCancelTime(LocalDateTime.now());
                ordersMapper.update(orders);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void deliveryTimeout(){
        log.info("凌晨1点，检查有没有还在派送的");
        List<Orders> ordersList = ordersMapper.getByStatus(Orders.DELIVERY_IN_PROGRESS);
        for (Orders orders : ordersList) {
            if (Duration.between(orders.getOrderTime(), LocalDateTime.now()).toHours() > 1){
                //已完成
                orders.setStatus(Orders.COMPLETED);
                ordersMapper.update(orders);
            }
        }
    }
}
