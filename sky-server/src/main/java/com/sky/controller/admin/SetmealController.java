package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @PostMapping
    @ApiOperation("新增套餐")
    @CacheEvict(cacheNames = "Setmeal", key = "setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO){

        log.info("新增套餐: {}", setmealDTO);
        setmealService.save(setmealDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("套餐分页查询: {}", setmealPageQueryDTO);

        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("套餐批量删除")
    @CacheEvict(cacheNames = "Setmeal", allEntries = true)
    public Result deleteBatch(@RequestParam List<Long> ids){
        log.info("套餐批量删除: {}", ids);

        setmealService.deleteBatch(ids);

        return Result.success();
    }

    @PutMapping
    @ApiOperation("更新套餐")
    @CacheEvict(cacheNames = "Setmeal", allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO){

        log.info("更新套餐: {}", setmealDTO);

        setmealService.uapdate(setmealDTO);

        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据套餐id获取套餐VO")
    public Result<SetmealVO> getVOById(@PathVariable Long id){

        log.info("根据套餐id获取套餐VO: {}", id);

        SetmealVO setmealVO = setmealService.getVOById(id);

        return Result.success(setmealVO);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("起售停售套餐")
    @CacheEvict(cacheNames = "Setmeal", allEntries = true)
    public Result StartOrStop(@PathVariable Integer status, Long id){
        log.info("把id为: {}的菜品状态设置为: {}", id, status);

        setmealService.StartOrStop(status, id);

        return Result.success();
    }
}
