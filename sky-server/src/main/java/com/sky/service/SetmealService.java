package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * 新增套餐
     * @param setmealDTO
     */
    void save(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 套餐批量删除
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 更新套餐
     * @param setmealDTO
     */
    void uapdate(SetmealDTO setmealDTO);

    /**
     * 根据套餐id获取套餐VO
     * @param id
     * @return
     */
    SetmealVO getVOById(Long id);

    /**
     * 更改套餐售卖状态
     * @param status
     * @param id
     */
    void StartOrStop(Integer status, Long id);
}
