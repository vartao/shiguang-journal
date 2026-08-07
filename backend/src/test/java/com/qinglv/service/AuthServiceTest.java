package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.common.JwtUtil;
import com.qinglv.dto.RegisterDTO;
import com.qinglv.mapper.CoupleMapper;
import com.qinglv.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private CoupleMapper coupleMapper;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void concurrentDuplicateUsernameReturnsBusinessError() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice");
        dto.setPassword("secret123");
        dto.setNickname("Alice");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_username"));

        BusinessException error = assertThrows(BusinessException.class, () -> authService.register(dto));

        assertEquals("用户名已存在", error.getMessage());
    }
}
