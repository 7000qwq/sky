package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.mapper.OrdersMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "订单相关接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrdersMapper ordersMapper;

    @PostMapping("/submit")
    @ApiOperation("用户下单接口")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submit(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);

        /*
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
         */

        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());

        OrderPaymentVO orderPaymentVO = new OrderPaymentVO();

        return Result.success(orderPaymentVO);
    }

    @GetMapping("historyOrders")
    @ApiOperation("分页查询历史订单")
    public Result<PageResult> queryHistory(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("订单分页查询: {}", ordersPageQueryDTO);
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);
        PageResult pageResult = orderService.pageQuery(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> queryOrderDetail(@PathVariable Long id){
        log.info("查询订单: {}详情", id);
        OrderVO orderVO = orderService.queryOrderDetail(id);
        return Result.success(orderVO);
    }

    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancelOrder(@PathVariable Long id){

        log.info("取消订单: {}", id);

        orderService.cancelOrder(id);

        return Result.success();
    }

    @PostMapping("repetition/{id}")
    @ApiOperation("再来一单")
    public Result againOrder(@PathVariable Long id){
        log.info("再来一单: {}", id);
        orderService.againOrder(id);
        return Result.success();
    }

    @GetMapping("reminder/{id}")
    @ApiOperation("客户催单")
    public Result reminder(@PathVariable Long id){
        log.info("客户催单: {}", id);
        orderService.remind(id);
        return Result.success();
    }
}
