package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MessageDTO {

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 500, message = "留言不能超过500字")
    private String content;
}
