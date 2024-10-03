package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {


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

        orders.setOrderTime(LocalDateTime.now());
        orders.setUserId(userId);
        orders.setUserName(userMapper.getNameById(userId));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));

        AddressBook addressBook = addressBookMapper.getById(orders.getAddressBookId());
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

        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);

        Page<OrderVO> orderVOList = ordersMapper.page(ordersPageQueryDTO);
        for (OrderVO orderVO : orderVOList) {

            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderVO.getId());
            orderVO.setOrderDetailList(orderDetailList);

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
}
