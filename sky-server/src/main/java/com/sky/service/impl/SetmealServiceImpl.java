package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    DishMapper dishMapper;

    @Autowired
    SetmealMapper setmealMapper;

    @Autowired
    SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {

        //套餐名称唯一，但是应该唯一的菜品名也没判断，就这样吧
        //套餐必须属于某个分类
        //套餐必须包含菜品
        //名称、分类、价格、图片为必填项
        //添加菜品窗口需要根据分类类型来展示菜品，这是别的接口，不用在这里调用

        //向setmeal表插入
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //新增的套餐默认为停售状态，这特么是必传的参数啊？
        //setmeal.setStatus(StatusConstant.DISABLE);
        setmealMapper.save(setmeal);

        //向setmeal_dish表插入
        Long id = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(id);
                    }
            );
            setmealDishMapper.save(setmealDishes);
        }

    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> setmealVOS = setmealMapper.page(setmealPageQueryDTO);

        List<SetmealVO> result = setmealVOS.getResult();
        long total = setmealVOS.getTotal();

        return new PageResult(total, result);
    }

    /**
     * 套餐批量删除
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        
        //判断当前套餐状态是否为起售中
        for (int i = 0; i < ids.size(); i++) {
            Setmeal setmeal = setmealMapper.getById(ids.get(i));
            if(setmeal.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //在setmeal表中删除数据
        setmealMapper.deleteBatch(ids);
        
        //在setmeal_dish表中删除数据
        setmealDishMapper.deleteBatch(ids);
    }

    /**
     * 更新套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void uapdate(SetmealDTO setmealDTO) {

        //更新setmeal表数据
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        //更新setmeal_dish表数据
        //先删除
        Long id = setmeal.getId();
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        setmealDishMapper.deleteBatch(ids);
        //后新增
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(
                    setmealDish -> {
                        setmealDish.setSetmealId(id);
                    }
            );

            setmealDishMapper.save(setmealDishes);
        }
    }

    @Override
    @Transactional
    public SetmealVO getVOById(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO= new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);

        setmealVO.setSetmealDishes(setmealDishMapper.getBySetmealId(id));

        return setmealVO;
    }

    @Override
    public void StartOrStop(Integer status, Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        setmeal.setStatus(status);
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
