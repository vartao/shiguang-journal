package com.qinglv.dto;

import lombok.Data;

/**
 * 登录/注册响应VO
 * 同时提供扁平字段 + nested user，兼容前后端两种读取方式
 */
@Data
public class LoginVO {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 所属情侣空间ID（可能为null）
     */
    private Long coupleId;

    /**
     * 角色：owner / member
     */
    private String role;

    /**
     * 嵌套用户信息（前端 store 优先使用）
     */
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long id;
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;
        private Long coupleId;
        private String role;
        private Integer gender;
    }
}
