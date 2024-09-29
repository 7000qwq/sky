package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 找出包含任一所选菜品id的所有套餐id
     * @param ids
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> ids);

    /**
     * 新增套餐时，在setmeal_dish表插入数据
     * @param setmealDishes
     */
    void save(List<SetmealDish> setmealDishes);
}
