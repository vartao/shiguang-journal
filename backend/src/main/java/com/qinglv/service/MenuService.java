package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.DishDTO;
import com.qinglv.entity.Dish;
import com.qinglv.entity.MenuMeal;
import com.qinglv.mapper.DishMapper;
import com.qinglv.mapper.MenuMealMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class MenuService {

    private static final Set<String> MEAL_TYPES = Set.of("breakfast", "lunch", "dinner");
    private static final Set<String> DISH_CATEGORIES =
            Set.of("breakfast", "lunch", "dinner", "night", "snack", "drink");

    private final DishMapper dishMapper;
    private final MenuMealMapper menuMealMapper;

    /**
     * 菜品池列表（按创建时间倒序）
     */
    public List<Dish> listDishes(Long coupleId, String category) {
        requireCouple(coupleId);

        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dish::getCoupleId, coupleId);
        if (StringUtils.hasText(category)) {
            wrapper.eq(Dish::getCategory, category);
        }
        wrapper.orderByDesc(Dish::getCreateTime);
        return dishMapper.selectList(wrapper);
    }

    /**
     * 添加菜品
     */
    public Dish createDish(Long userId, Long coupleId, DishDTO dto) {
        requireCouple(coupleId);

        Dish dish = new Dish();
        dish.setCoupleId(coupleId);
        dish.setUserId(userId);
        dish.setName(dto.getName().trim());
        dish.setCategory(
                StringUtils.hasText(dto.getCategory()) && DISH_CATEGORIES.contains(dto.getCategory())
                        ? dto.getCategory()
                        : "lunch");
        dish.setNote(dto.getNote());

        dishMapper.insert(dish);
        return dish;
    }

    /**
     * 删除菜品（情侣空间内双方都可删除）
     */
    public void deleteDish(Long id, Long coupleId) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        if (!Objects.equals(dish.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权删除该菜品");
        }
        dishMapper.deleteById(id);
    }

    /**
     * 从菜品池随机抽一道
     */
    public Dish randomDish(Long coupleId, String category) {
        List<Dish> dishes = listDishes(coupleId, category);
        if (dishes.isEmpty()) {
            throw new BusinessException("菜品池还是空的，先去加几道想吃的菜吧");
        }
        return dishes.get(ThreadLocalRandom.current().nextInt(dishes.size()));
    }

    /**
     * 获取某天的三餐菜单
     */
    public List<MenuMeal> getMenu(Long coupleId, LocalDate mealDate) {
        requireCouple(coupleId);

        LambdaQueryWrapper<MenuMeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuMeal::getCoupleId, coupleId)
                .eq(MenuMeal::getMealDate, mealDate)
                .orderByAsc(MenuMeal::getCreateTime);
        return menuMealMapper.selectList(wrapper);
    }

    /**
     * 给某天某餐次加一道菜（同日同餐次同名幂等，直接返回已有记录）
     */
    public MenuMeal addMenuItem(Long userId, Long coupleId, LocalDate mealDate, String mealType, String dishName) {
        requireCouple(coupleId);
        if (mealDate == null) {
            throw new BusinessException("日期不能为空");
        }
        if (!MEAL_TYPES.contains(mealType)) {
            throw new BusinessException("餐次只能是 breakfast/lunch/dinner");
        }
        if (!StringUtils.hasText(dishName)) {
            throw new BusinessException("菜名不能为空");
        }
        String name = dishName.trim();

        LambdaQueryWrapper<MenuMeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuMeal::getCoupleId, coupleId)
                .eq(MenuMeal::getMealDate, mealDate)
                .eq(MenuMeal::getMealType, mealType)
                .eq(MenuMeal::getDishName, name);
        MenuMeal existing = menuMealMapper.selectOne(wrapper);
        if (existing != null) {
            return existing;
        }

        MenuMeal item = new MenuMeal();
        item.setCoupleId(coupleId);
        item.setUserId(userId);
        item.setMealDate(mealDate);
        item.setMealType(mealType);
        item.setDishName(name);
        menuMealMapper.insert(item);
        return item;
    }

    /**
     * 删除菜单中的一项
     */
    public void deleteMenuItem(Long id, Long coupleId) {
        MenuMeal item = menuMealMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("菜单项不存在");
        }
        if (!Objects.equals(item.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权删除该菜单项");
        }
        menuMealMapper.deleteById(id);
    }

    private void requireCouple(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
    }
}
