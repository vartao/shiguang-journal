package com.qinglv.common;

import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrentUserContextTest {

    @Test
    void loadsCurrentUserOncePerRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUserContext.USER_ID_ATTRIBUTE, 7L);
        UserMapper userMapper = mock(UserMapper.class);
        User user = new User();
        user.setId(7L);
        user.setCoupleId(11L);
        when(userMapper.selectById(7L)).thenReturn(user);

        CurrentUserContext context = new CurrentUserContext(request, userMapper);

        assertEquals(7L, context.getUserId());
        assertSame(user, context.getUser());
        assertEquals(11L, context.getCoupleId());
        assertEquals(11L, context.getCoupleId());
        verify(userMapper).selectById(7L);
    }

    @Test
    void missingIdentityDoesNotQueryUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserMapper userMapper = mock(UserMapper.class);
        CurrentUserContext context = new CurrentUserContext(request, userMapper);

        assertNull(context.getUserId());
        assertNull(context.getUser());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void validTokenButDeletedUserThrowsUnauthorized() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUserContext.USER_ID_ATTRIBUTE, 99L);
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.selectById(99L)).thenReturn(null);

        CurrentUserContext context = new CurrentUserContext(request, userMapper);

        assertThrows(UnauthorizedException.class, context::getCoupleId);
        assertThrows(UnauthorizedException.class, context::requireUser);
    }
}
