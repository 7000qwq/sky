package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    private DishService dishService;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){

        log.info("新增菜品:{}", dishDTO);

        dishService.save(dishDTO);
        cleanByCategoryId(dishDTO.getCategoryId());
        return Result.success();
    }


    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){

        log.info("菜品分页查询：{}", dishPageQueryDTO);

        PageResult pageResult = dishService.page(dishPageQueryDTO);

        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result deleteBatch(@RequestParam List<Long> ids){

        log.info("菜品批量删除：{}", ids);

        dishService.deleteBatch(ids);
        cleanAll();
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){

        log.info("根据id查询菜品：{}", id);

        DishVO dishVO = dishService.getById(id);

        return Result.success(dishVO);
    }

    @PutMapping
    @ApiOperation("更新菜品数据")
    public Result update(@RequestBody DishDTO dishDTO){

        log.info("更新菜品数据: {}", dishDTO);

        dishService.update(dishDTO);
        cleanAll();
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品List")
    public Result<List<Dish>> getByCategoryId(Long categoryId){

        log.info("根据分类id查询菜品List: {}", categoryId);

        List<Dish> dishList = dishService.getByCategoryId(categoryId);

        return Result.success(dishList);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("更改菜品售卖状态")
    public Result StartOrStop(@PathVariable Integer status, Long id){
        log.info("更改id为: {}的菜品售卖状态为: {}", id, status);
        dishService.StartOrStop(status, id);
        cleanAll();
        return Result.success();
    }

    public void cleanByCategoryId(Long categoryId){
        redisTemplate.delete("dish_" + categoryId.toString());
    }

    public void cleanAll(){
        //删除不能直接用通配符
        Set keys = redisTemplate.keys("dish_*");
        redisTemplate.delete(keys);
    }
}
