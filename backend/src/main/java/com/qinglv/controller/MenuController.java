package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.dto.DishDTO;
import com.qinglv.dto.MenuItemDTO;
import com.qinglv.entity.Dish;
import com.qinglv.entity.MenuMeal;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final UserMapper userMapper;

    @GetMapping("/api/dishes")
    public Result<List<Dish>> getDishes(@RequestParam(required = false) String category) {
        return Result.success(menuService.listDishes(currentCoupleId(), category));
    }

    @PostMapping("/api/dishes")
    public Result<Dish> createDish(@Valid @RequestBody DishDTO dto) {
        return Result.success("菜品已加入池中", menuService.createDish(currentUserId(), currentCoupleId(), dto));
    }

    @DeleteMapping("/api/dishes/{id}")
    public Result<Void> deleteDish(@PathVariable Long id) {
        menuService.deleteDish(id, currentCoupleId());
        return Result.success("删除成功", null);
    }

    @GetMapping("/api/dishes/random")
    public Result<Dish> randomDish(@RequestParam(required = false) String category) {
        return Result.success(menuService.randomDish(currentCoupleId(), category));
    }

    @GetMapping("/api/menu")
    public Result<List<MenuMeal>> getMenu(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate mealDate = date != null ? date : LocalDate.now();
        return Result.success(menuService.getMenu(currentCoupleId(), mealDate));
    }

    @PostMapping("/api/menu")
    public Result<MenuMeal> addMenuItem(@Valid @RequestBody MenuItemDTO dto) {
        MenuMeal item = menuService.addMenuItem(
                currentUserId(), currentCoupleId(), dto.getMealDate(), dto.getMealType(), dto.getDishName());
        return Result.success("已加入菜单", item);
    }

    @DeleteMapping("/api/menu/{id}")
    public Result<Void> deleteMenuItem(@PathVariable Long id) {
        menuService.deleteMenuItem(id, currentCoupleId());
        return Result.success("删除成功", null);
    }

    private Long currentUserId() {
        return JwtInterceptor.getCurrentUserId();
    }

    private Long currentCoupleId() {
        User user = userMapper.selectById(currentUserId());
        return user != null ? user.getCoupleId() : null;
    }
}
