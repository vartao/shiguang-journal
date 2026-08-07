package com.qinglv.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 上传照片请求DTO
 */
@Data
public class PhotoDTO {

    /**
     * 所属相册ID
     */
    @NotNull(message = "相册ID不能为空")
    private Long albumId;

    /**
     * 照片描述
     */
    private String description;
}
