package com.sky.service.impl;

import com.sky.dto.DataOverViewQueryDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param beginDate, endDate
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate beginDate, LocalDate endDate) {

        String dateList = "";
        String turnoverList = "";

        //遍历 开始 和 结束 之间的所有日期
        for (LocalDate date = beginDate; !date.isAfter(endDate); date = date.plusDays(1)){

            //把每个日期的Order取出来
            List<Orders> ordersList = ordersMapper.getByDate(date);

            BigDecimal sum = BigDecimal.valueOf(0);

            for (Orders orders : ordersList) {
                //把其中状态为已完成的Order金额累积
                if (orders.getStatus() == Orders.COMPLETED )
                    sum = sum.add(orders.getAmount());
            }

            dateList = dateList + date.toString() + ",";
            turnoverList = turnoverList + sum.toString() + ",";
        }

        log.info("日期为: {}", dateList);
        log.info("营业额为: {}", turnoverList);

        if (dateList.length() > 0) {
            dateList = dateList.substring(0, dateList.length() - 1);
        }
        if (turnoverList.length() > 0) {
            turnoverList = turnoverList.substring(0, turnoverList.length() - 1);
        }
        return new TurnoverReportVO(dateList, turnoverList);
    }

    /*
    新增用户统计
     */
    @Override
    public UserReportVO userStatistics(LocalDate beginDate, LocalDate endDate) {
        String dateList = "";
        String newUserList = "";
        String totalUserList = "";

        int sum = userMapper.getByBeforeDate(beginDate);

        //遍历 开始 和 结束 之间的所有日期
        for (LocalDate date = beginDate; !date.isAfter(endDate); date = date.plusDays(1)){

            //把每个日期的User取出来
            List<User> userList = userMapper.getByDate(date);

            int newUserNum = 0;
            if (userList != null){
                newUserNum = userList.size();
            }

            sum += newUserNum;

            dateList = dateList + date.toString() + ",";
            newUserList = newUserList + newUserNum + ",";
            totalUserList = totalUserList + sum + ",";
        }

        log.info("日期为: {}", dateList);
        log.info("新增用户为: {}", newUserList);
        log.info("总用户为: {}", totalUserList);

        if (dateList.length() > 0) {
            dateList = dateList.substring(0, dateList.length() - 1);
        }
        if (newUserList.length() > 0) {
            newUserList = newUserList.substring(0, newUserList.length() - 1);
        }
        if (totalUserList.length() > 0) {
            totalUserList = totalUserList.substring(0, totalUserList.length() - 1);
        }
        return new UserReportVO(dateList, totalUserList, newUserList);
    }

    /**
     * 订单统计
     * @param beginDate
     * @param endDate
     * @return
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate beginDate, LocalDate endDate) {
        String dateList = "";
        String OrderNumList = "";
        String OrderComNumList = "";
        Integer orderSum = 0;
        Integer orderComSum = 0;

        //遍历 开始 和 结束 之间的所有日期
        for (LocalDate date = beginDate; !date.isAfter(endDate); date = date.plusDays(1)){

            //把每个日期的Order取出来
            List<Orders> ordersList = ordersMapper.getByDate(date);

            int orderNum = ordersList.size();
            int orderComNum = 0;
            orderSum += orderNum;

            for (Orders orders : ordersList) {
                //把其中状态为已完成的Order金额累积
                if (orders.getStatus() == Orders.COMPLETED ){
                    orderComNum += 1;
                }

            }

            orderComSum += orderComNum;
            dateList = dateList + date.toString() + ",";
            OrderNumList = OrderNumList + orderNum + ",";
            OrderComNumList = OrderComNumList + orderComNum + ",";
        }

        log.info("日期为: {}", dateList);
        log.info("营业额为: {}", OrderNumList);
        log.info("营业额为: {}", OrderComNumList);

        if (dateList.length() > 0) {
            dateList = dateList.substring(0, dateList.length() - 1);
        }
        if (OrderNumList.length() > 0) {
            OrderNumList = OrderNumList.substring(0, OrderNumList.length() - 1);
        }
        if (OrderComNumList.length() > 0) {
            OrderComNumList = OrderComNumList.substring(0, OrderComNumList.length() - 1);
        }

        Double com = 0.0;
        if (orderSum != 0){
            com = orderComSum.doubleValue() / orderSum.doubleValue();
        }

        return new OrderReportVO(dateList, OrderNumList, OrderComNumList, orderSum, orderComSum, com);
    }
}
