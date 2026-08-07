package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
public class RegisterDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度为3-20个字符")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32个字符")
    private String password;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称最长20个字符")
    private String nickname;

    /**
     * 性别：0-保密 1-男 2-女
     */
    private Integer gender;

    /**
     * 前端角色选择兼容：boy / girl（会映射到 gender）
     */
    private String role;

    /**
     * 情侣空间邀请码（可选，有则加入已有空间）
     */
    private String inviteCode;

    /**
     * 在一起日期（创建空间时使用，格式 yyyy-MM-dd）
     */
    private String togetherDate;
}
