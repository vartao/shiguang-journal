package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.dto.DishDTO;
import com.qinglv.dto.MenuItemDTO;
import com.qinglv.entity.Dish;
import com.qinglv.entity.MenuMeal;
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
    private final CurrentUserContext currentUser;

    @GetMapping("/api/dishes")
    public Result<List<Dish>> getDishes(@RequestParam(required = false) String category) {
        return Result.success(menuService.listDishes(currentUser.getCoupleId(), category));
    }

    @PostMapping("/api/dishes")
    public Result<Dish> createDish(@Valid @RequestBody DishDTO dto) {
        return Result.success(
                "菜品已加入池中",
                menuService.createDish(currentUser.getUserId(), currentUser.getCoupleId(), dto));
    }

    @DeleteMapping("/api/dishes/{id}")
    public Result<Void> deleteDish(@PathVariable Long id) {
        menuService.deleteDish(id, currentUser.getCoupleId());
        return Result.success("删除成功", null);
    }

    @GetMapping("/api/dishes/random")
    public Result<Dish> randomDish(@RequestParam(required = false) String category) {
        return Result.success(menuService.randomDish(currentUser.getCoupleId(), category));
    }

    @GetMapping("/api/menu")
    public Result<List<MenuMeal>> getMenu(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate mealDate = date != null ? date : LocalDate.now();
        return Result.success(menuService.getMenu(currentUser.getCoupleId(), mealDate));
    }

    @PostMapping("/api/menu")
    public Result<MenuMeal> addMenuItem(@Valid @RequestBody MenuItemDTO dto) {
        MenuMeal item = menuService.addMenuItem(
                currentUser.getUserId(),
                currentUser.getCoupleId(),
                dto.getMealDate(),
                dto.getMealType(),
                dto.getDishName());
        return Result.success("已加入菜单", item);
    }

    @DeleteMapping("/api/menu/{id}")
    public Result<Void> deleteMenuItem(@PathVariable Long id) {
        menuService.deleteMenuItem(id, currentUser.getCoupleId());
        return Result.success("删除成功", null);
    }
}
