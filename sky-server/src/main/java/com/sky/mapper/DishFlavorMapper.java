package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入口味数据
     * @param flavors
     */
    void insertBatch(List<DishFlavor> flavors);

    /**
     * 根据菜品id来删除口味表数据
     * @param dishId
     */
    @Delete("delete from dish_flavor where dish_id = #{dishId}")
    void delete(Long dishId);

    /**
     * 批量删除
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据菜品id查询口味
     * @param id
     * @return
     */
    @Select("select * from dish_flavor where dish_id = #{id}")
    List<DishFlavor> getByDishId(Long id);

    /**
     * 菜品更新时更新口味
     * @param flavors
     */
    void update(List<DishFlavor> flavors);
}
