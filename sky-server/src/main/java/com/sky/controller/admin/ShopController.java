package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags = "店铺管理接口")
public class ShopController {

    @Autowired
    private RedisTemplate redisTemplate;

    public static final String KEY = "statu";

    /**
     * 店铺状态设置
     */
    @PutMapping("/{status}")
    @ApiOperation("店铺状态设置")
    public Result setStatu(@PathVariable Integer status){
        log.info("设置店铺状态为: {}", status == 1 ? "营业中" : "打烊中");

        redisTemplate.opsForValue().set(KEY, status);

        return Result.success();
    }


    /**
     * 店铺状态查询
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("店铺状态查询")
    public Result<Integer> getStatu(){

        Integer statu = (Integer) redisTemplate.opsForValue().get(KEY);

        log.info("店铺状态查询结果为: {}", statu == 1 ? "营业中" : "打烊中");

        return Result.success(statu);
    }
}
