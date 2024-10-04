package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final String getLocationURL = "https://api.map.baidu.com/geocoding/v3";

    private static final String getScaleURL = "https://api.map.baidu.com/directionlite/v1/riding";

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.shop.scale}")
    private Integer scale;

    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        Long userId = BaseContext.getCurrentId();

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);

        //处理异常
        //地址簿为空
        if (addressBookMapper.getById(orders.getAddressBookId()) == null){
            throw new RuntimeException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //购物车为空
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.select(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0){
            throw new RuntimeException(MessageConstant.SHOPPING_CART_IS_NULL);
        }


        AddressBook addressBook = addressBookMapper.getById(orders.getAddressBookId());
        String userAddress = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();

        //超出配送范围
        if ( tooFarToDelivery(userAddress) ){
            log.info("无法配送！");
            throw new OrderBusinessException(MessageConstant.TOO_FAR_TO_DELIVERY);
        }

        orders.setOrderTime(LocalDateTime.now());
        orders.setUserId(userId);
        orders.setUserName(userMapper.getNameById(userId));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));


        orders.setAddress(addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());

        //order表管理每个订单，一个订单存1条，id就是第二个表中的order_id；先存到这个表中，然后再存第二个表
        ordersMapper.insert(orders);

        //order_detail表中，其实购物车每条数据，在这里存一条
        Long ordersId = orders.getId();


        for (ShoppingCart cart : shoppingCartList) {

            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(ordersId);

            orderDetailMapper.insert(orderDetail);//可以优化为批量插入

        }
        shoppingCartMapper.deleteByUserId(userId);


        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setOrderTime(orders.getOrderTime());
        orderSubmitVO.setId(orders.getId());
        orderSubmitVO.setOrderAmount(orders.getAmount());
        orderSubmitVO.setOrderNumber(orders.getNumber());

        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = ordersMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        ordersMapper.update(orders);
    }

    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<OrderVO> orderVOList = ordersMapper.page(ordersPageQueryDTO);
        for (OrderVO orderVO : orderVOList) {

            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderVO.getId());
            orderVO.setOrderDetailList(orderDetailList);

            String orderDishes = "";
            for (OrderDetail orderDetail : orderDetailList) {
                orderDishes = orderDishes + orderDetail.getName() + '*' + orderDetail.getNumber() + ';';
            }

            orderVO.setOrderDishes(orderDishes);

        }

        PageResult pageResult = new PageResult();
        pageResult.setTotal(orderVOList.getTotal());
        pageResult.setRecords(orderVOList.getResult());

        return pageResult;
    }

    @Override
    public OrderVO queryOrderDetail(Long id) {

        Orders orders = ordersMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        OrderVO orderVO = new OrderVO("", orderDetailList);
        BeanUtils.copyProperties(orders, orderVO);

        return orderVO;
    }

    @Override
    public void cancelOrder(Long orderId) {

        Orders orders = ordersMapper.getById(orderId);

        //处理异常：订单不存在
        if (orders == null){
            throw new RuntimeException(MessageConstant.ORDER_NOT_FOUND);
        }

        //处理异常：订单状态错误，已经接单
        if (orders.getStatus() > 2){
            throw new RuntimeException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //记录订单取消原因、取消时间
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());

        //修改订单状态为已取消
        orders.setStatus(Orders.CANCELLED);

        //退款，此处应有微信支付接口

        //修改订单支付状态为已退款
        orders.setPayStatus(Orders.REFUND);

        ordersMapper.update(orders);
    }

    /**
     * 根据订单id再来一单
     * @param id
     */
    @Override
    public void againOrder(Long id) {


        //前端已经写好了点击“再来一单”，就先清空购物车的逻辑

        List<OrderDetail> byOrderId = orderDetailMapper.getByOrderId(id);
        if(byOrderId != null && byOrderId.size() > 0){

            List<ShoppingCart> shoppingCartList = new ArrayList<>();

            for (OrderDetail orderDetail : byOrderId) {
                ShoppingCart shoppingCart = new ShoppingCart();
                BeanUtils.copyProperties(orderDetail, shoppingCart);
                shoppingCart.setUserId(BaseContext.getCurrentId());
                shoppingCartList.add(shoppingCart);
            }

            shoppingCartMapper.insertBatch(shoppingCartList);
        }

    }

    @Override
    public OrderStatisticsVO statistics() {

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();

        Integer status = Orders.CONFIRMED;
        List<Orders> confirmed_list = ordersMapper.getByStatus(status);
        orderStatisticsVO.setConfirmed(confirmed_list.size());

        status = Orders.DELIVERY_IN_PROGRESS;
        List<Orders> delivery_list = ordersMapper.getByStatus(status);
        orderStatisticsVO.setDeliveryInProgress(delivery_list.size());

        status = Orders.TO_BE_CONFIRMED;
        List<Orders> toBeConfirmed_list = ordersMapper.getByStatus(status);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed_list.size());

        return orderStatisticsVO;
    }

    @Override
    public void confirm(Long id) {
        Orders orders = ordersMapper.getById(id);
        orders.setStatus(Orders.CONFIRMED);
        ordersMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {

        Orders orders = ordersMapper.getById(ordersRejectionDTO.getId());
        //校验订单状态是否为待接单
        if (orders.getStatus() != Orders.TO_BE_CONFIRMED){
            throw new RuntimeException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态为已取消、设置取消原因、时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersRejectionDTO.getRejectionReason());

        //微信接口退款

        //修改支付状态为已退款
        orders.setPayStatus(Orders.REFUND);

        ordersMapper.update(orders);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {

        Orders orders = ordersMapper.getById(ordersCancelDTO.getId());

        //修改订单状态为已取消、设置取消原因、时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());

        //微信接口退款

        //修改支付状态为已退款
        orders.setPayStatus(Orders.REFUND);

        ordersMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orders = ordersMapper.getById(id);

        //校验订单状态是否为已接单
        if (orders.getStatus() != Orders.CONFIRMED){
            throw new RuntimeException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态为正在派送
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        ordersMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders orders = ordersMapper.getById(id);

        //校验订单状态是否为正在派送
        if (orders.getStatus() != Orders.DELIVERY_IN_PROGRESS){
            throw new RuntimeException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //修改订单状态为已完成
        orders.setStatus(Orders.COMPLETED);

        ordersMapper.update(orders);
    }


    private boolean tooFarToDelivery(String userAddress){

        //获取店铺经纬度
        Map<String, String> map1 = new HashMap<>();
        map1.put("address", shopAddress);
        map1.put("ak", ak);
        map1.put("output", "json");
        String json1 = HttpClientUtil.doGet(getLocationURL, map1);
        JSONObject jsonObject1 = JSON.parseObject(json1);
        Position shopPosition = new Position();
        shopPosition.setJingDu(jsonObject1.getJSONObject("result")
                .getJSONObject("location")
                .getDouble("lng"));
        shopPosition.setWeiDu(jsonObject1.getJSONObject("result")
                .getJSONObject("location")
                .getDouble("lat"));

        //获取用户经纬度
        Map<String, String> map2 = new HashMap<>();
        map2.put("address", userAddress);
        map2.put("ak", ak);
        map2.put("output", "json");
        String json2 = HttpClientUtil.doGet(getLocationURL, map2);
        JSONObject jsonObject2 = JSON.parseObject(json2);
        Position userPosition = new Position();
        userPosition.setJingDu(jsonObject2.getJSONObject("result")
                .getJSONObject("location")
                .getDouble("lng"));
        userPosition.setWeiDu(jsonObject2.getJSONObject("result")
                .getJSONObject("location")
                .getDouble("lat"));

        log.info("店铺经度为: {}, 纬度为: {}", shopPosition.getJingDu(), shopPosition.getWeiDu());
        log.info("用户经度为: {}, 纬度为: {}", userPosition.getJingDu(), userPosition.getWeiDu());

        //调用导航接口
        Map<String, String> map3 = new HashMap<>();
        map3.put("origin", String.valueOf(shopPosition.getWeiDu()) + "," + String.valueOf(shopPosition.getJingDu()));
        map3.put("destination", String.valueOf(userPosition.getWeiDu()) + "," + String.valueOf(userPosition.getJingDu()));
        map3.put("ak", ak);
        String json3 = HttpClientUtil.doGet(getScaleURL, map3);

        log.info("返回值为: {}",json3);

        JSONObject jsonObject3 = JSON.parseObject(json3);

        Integer status = jsonObject3.getInteger("status");
        log.info("status为: {}", status);
        if (status != 0){
            return true;
        }
        double distance = jsonObject3.getJSONObject("result")
                .getJSONArray("routes")
                .getJSONObject(0)
                .getDouble("distance");



        log.info("路线距离为: {}",distance);

        if(distance > scale){
            return true;
        }else {
            return false;
        }

    }



}
