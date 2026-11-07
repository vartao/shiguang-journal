package com.qinglv.common;

import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * 当前请求用户上下文，集中解析登录用户及其情侣空间。
 * <p>
 * 用户记录在单次请求内最多查询一次并记忆化；Service 可通过
 * {@link #getUser()} 复用该实体，避免重复查库。
 */
@Component
@RequestScope
@RequiredArgsConstructor
public class CurrentUserContext {

    static final String USER_ID_ATTRIBUTE = CurrentUserContext.class.getName() + ".USER_ID";

    private final HttpServletRequest request;
    private final UserMapper userMapper;

    private boolean userLoaded;
    private User currentUser;

    public Long getUserId() {
        return (Long) request.getAttribute(USER_ID_ATTRIBUTE);
    }

    /**
     * 当前登录用户实体（请求内缓存，可能为 {@code null}）。
     */
    public User getUser() {
        if (!userLoaded) {
            Long userId = getUserId();
            currentUser = userId != null ? userMapper.selectById(userId) : null;
            userLoaded = true;
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户实体；若 Token 有效但用户已不存在则抛出 401。
     */
    public User requireUser() {
        User user = getUser();
        if (user == null) {
            throw new UnauthorizedException("登录状态已失效，请重新登录");
        }
        return user;
    }

    /**
     * 当前用户所属情侣空间 ID；用户尚未加入空间时为 {@code null}。
     * Token 有效但用户已不存在时抛出 401。
     */
    public Long getCoupleId() {
        return requireUser().getCoupleId();
    }
}
