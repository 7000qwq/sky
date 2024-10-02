package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    @Transactional
    public void save(DishDTO dishDTO) {

        //向菜品表插入1条数据

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);

        Long dishId = dish.getId();

        //向口味表插入多条数据

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){

            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
                    }
            );
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    @Override
    public PageResult page(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        //为什么这里面放VO？？这样会把不需要return的口味list也返回啊，不需要就不需要，只要有需要的就行
        Page<DishVO> page = dishMapper.page(dishPageQueryDTO);

        Long total = page.getTotal();
        List<DishVO> pageResult = page.getResult();

        return new PageResult(total, pageResult);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        
        //判断是否停售
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish != null && dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        
        
        //判断是否被套餐包含
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);

        if(setmealIds != null && setmealIds.size() > 0){
            throw  new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }


        /*
        for (Long id : ids) {
            //删除菜品表数据
            dishMapper.delete(id);
            //删除口味表数据
            dishFlavorMapper.delete(id);
        }
         */

        //批量删除
        dishMapper.deleteBatch(ids);
        dishFlavorMapper.deleteBatch(ids);
    }

    @Override
    public DishVO getById(Long id) {

        DishVO dishVO = new DishVO();

        Dish dish = dishMapper.getById(id);

        BeanUtils.copyProperties(dish, dishVO);

        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);

        dishVO.setFlavors(flavors);

        return dishVO;
    }

    @Override
    @Transactional
    public void update(DishDTO dishDTO) {

        //更新菜品表数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        Long dishId = dish.getId();

        //删除原来口味表数据
        dishFlavorMapper.delete(dishId);

        //新增新的口味表数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){

            flavors.forEach(dishFlavor -> {
                        dishFlavor.setDishId(dishId);
                    }
            );
            //这是update，会把菜品id传过来，没必要set吧？
            //卧槽，为什么一定要set呢

            dishFlavorMapper.insertBatch(flavors);
        }
    }

    @Override
    public List<Dish> getByCategoryId(Long categoryId) {

        List<Dish> dishList = dishMapper.getByCategoryId(categoryId);

        return dishList;
    }

    @Override
    public void StartOrStop(Integer status, Long id) {
        Dish dish = dishMapper.getById(id);
        dish.setStatus(status);
        dishMapper.update(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.getByCategoryId(dish.getCategoryId());

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }


}
