package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/编辑相册请求DTO
 */
@Data
public class AlbumDTO {

    /**
     * 相册名称
     */
    @NotBlank(message = "相册名称不能为空")
    @Size(max = 50, message = "相册名称最长50个字符")
    private String name;

    /**
     * 相册描述
     */
    @Size(max = 200, message = "相册描述最长200个字符")
    private String description;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 前端兼容字段 cover
     */
    private String cover;
}
