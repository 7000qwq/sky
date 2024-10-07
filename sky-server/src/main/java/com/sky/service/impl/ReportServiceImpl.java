package com.sky.service.impl;

import com.sky.dto.DataOverViewQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private WorkspaceService workspaceService;

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

    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
        //先把时间段内订单取出
        List<Orders> ordersList  = ordersMapper.getByDateDur(begin, end);

        Map<String, Integer> dishes = new HashMap<>();
        //遍历这些订单，根据id查菜品，查到的菜品统计数 + 1
        for (Orders orders : ordersList) {
            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
            for (OrderDetail orderDetail : orderDetailList) {
                //用什么数据结构来存菜品统计数呢？键值对，根据值排序，键为name，值为number
                if (dishes.containsKey(orderDetail.getName())){
                    dishes.put(orderDetail.getName(), dishes.get(orderDetail.getName()) + orderDetail.getNumber());
                }else {
                    dishes.put(orderDetail.getName(), orderDetail.getNumber());
                }
            }
        }
        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(dishes.entrySet());
        entryList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        System.out.println(entryList);

        String namelist = "";
        String numList = "";
        for (int i = 0; i < 10 && i < entryList.size(); i ++){
            namelist = namelist + entryList.get(i).getKey() + ",";
            numList = numList + entryList.get(i).getValue() + ",";
        }

        if (namelist.length() > 0) {
            namelist = namelist.substring(0, namelist.length() - 1);
        }
        if (numList.length() > 0) {
            numList = numList.substring(0, numList.length() - 1);
        }

        return new SalesTop10ReportVO(namelist, numList);
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
