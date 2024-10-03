package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {

        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        shoppingCart.setUserId(BaseContext.getCurrentId());

        //先查一模一样的项有没有？如果有就数量+1不用重复插入？

        List<ShoppingCart> list = shoppingCartMapper.select(shoppingCart);
        if(list != null && list.size() > 0){
            //有一样的，update需要更新数量
            log.info("更新购物车原数量： {}", list.get(0));
            shoppingCartMapper.update(list.get(0));

        } else {
            //没一样的，insert

            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setNumber(1);

            if (shoppingCart.getDishId() != null){
                //加的是菜品
                Dish dish = dishMapper.getById(shoppingCart.getDishId());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                log.info("添加菜品到购物车：{}", shoppingCart);

            } else if (shoppingCart.getSetmealId() != null){
                //加的是套餐
                Setmeal setmeal = setmealMapper.getById(shoppingCart.getSetmealId());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                log.info("添加套餐到购物车： {}", shoppingCart);
            }

            shoppingCartMapper.insert(shoppingCart);
        }

    }

    @Override
    public List<ShoppingCart> getByUserId() {

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.select(shoppingCart);

        return list;
    }

    @Override
    public void cleanAll() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {

        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> list = shoppingCartMapper.select(shoppingCart);

        if( list != null && list.size() > 0){

            ShoppingCart shoppingCart1 = list.get(0);

            if (shoppingCart1.getNumber() == 1){

                //只剩1个了，需要删除这条数据
                shoppingCartMapper.deleteById(shoppingCart1.getId());

            } else {

                //还有大于1个
                shoppingCartMapper.updateSub(shoppingCart1.getId());
            }

        }

    }
}
