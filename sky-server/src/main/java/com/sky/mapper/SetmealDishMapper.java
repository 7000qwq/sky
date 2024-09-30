package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 批量删除setmeal_dish表数据
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据套餐id取出所有套餐-菜品关系
     * @param id
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{id}")
    List<SetmealDish> getBySetmealId(Long id);
}
