package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.entity.Dish;
import com.qinglv.entity.MenuMeal;
import com.qinglv.mapper.DishMapper;
import com.qinglv.mapper.MenuMealMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private DishMapper dishMapper;
    @Mock
    private MenuMealMapper menuMealMapper;

    @InjectMocks
    private MenuService menuService;

    @Test
    void randomDishFromEmptyPoolIsRejected() {
        when(dishMapper.selectList(any())).thenReturn(Collections.emptyList());

        BusinessException error = assertThrows(
                BusinessException.class, () -> menuService.randomDish(100L, null));

        assertEquals("菜品池还是空的，先去加几道想吃的菜吧", error.getMessage());
    }

    @Test
    void randomDishPicksFromThePool() {
        Dish dish = new Dish();
        dish.setId(1L);
        dish.setCoupleId(100L);
        dish.setName("番茄炒蛋");
        when(dishMapper.selectList(any())).thenReturn(List.of(dish));

        Dish picked = menuService.randomDish(100L, null);

        assertSame(dish, picked);
    }

    @Test
    void addingDuplicateMenuItemReturnsTheExistingOne() {
        MenuMeal existing = new MenuMeal();
        existing.setId(7L);
        existing.setCoupleId(100L);
        existing.setMealDate(LocalDate.of(2026, 8, 8));
        existing.setMealType("lunch");
        existing.setDishName("番茄炒蛋");
        when(menuMealMapper.selectOne(any())).thenReturn(existing);

        MenuMeal result = menuService.addMenuItem(
                1L, 100L, LocalDate.of(2026, 8, 8), "lunch", "番茄炒蛋");

        assertSame(existing, result);
        verify(menuMealMapper, never()).insert(any(MenuMeal.class));
    }

    @Test
    void addingNewMenuItemInserts() {
        when(menuMealMapper.selectOne(any())).thenReturn(null);

        MenuMeal result = menuService.addMenuItem(
                1L, 100L, LocalDate.of(2026, 8, 8), "dinner", "红烧肉");

        assertEquals("红烧肉", result.getDishName());
        assertEquals("dinner", result.getMealType());
        verify(menuMealMapper).insert(result);
    }

    @Test
    void addingMenuItemWithInvalidMealTypeIsRejected() {
        BusinessException error = assertThrows(
                BusinessException.class,
                () -> menuService.addMenuItem(1L, 100L, LocalDate.of(2026, 8, 8), "brunch", "煎饼"));

        assertEquals("餐次只能是 breakfast/lunch/dinner", error.getMessage());
        verify(menuMealMapper, never()).insert(any(MenuMeal.class));
    }

    @Test
    void listingDishesWithoutCoupleIsRejected() {
        BusinessException error = assertThrows(
                BusinessException.class, () -> menuService.listDishes(null, null));

        assertEquals("尚未加入情侣空间", error.getMessage());
    }

    @Test
    void deletingDishFromAnotherCoupleIsRejectedWithoutMutation() {
        Dish dish = new Dish();
        dish.setId(3L);
        dish.setCoupleId(999L);
        when(dishMapper.selectById(3L)).thenReturn(dish);

        BusinessException error = assertThrows(
                BusinessException.class, () -> menuService.deleteDish(3L, 100L));

        assertEquals(403, error.getCode());
        verify(dishMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deletingMenuItemFromAnotherCoupleIsRejectedWithoutMutation() {
        MenuMeal item = new MenuMeal();
        item.setId(5L);
        item.setCoupleId(999L);
        when(menuMealMapper.selectById(5L)).thenReturn(item);

        BusinessException error = assertThrows(
                BusinessException.class, () -> menuService.deleteMenuItem(5L, 100L));

        assertEquals(403, error.getCode());
        verify(menuMealMapper, never()).deleteById(any(Long.class));
    }
}
