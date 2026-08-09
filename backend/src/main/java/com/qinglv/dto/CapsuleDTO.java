package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CapsuleDTO {

    @NotBlank(message = "胶囊标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @Size(max = 5000, message = "信件内容不能超过5000字")
    private String message;

    @NotNull(message = "开启日期不能为空")
    private LocalDate openDate;

    /** 照片URL列表，服务端序列化为JSON字符串存储 */
    @Size(max = 6, message = "照片最多6张")
    private List<String> photoUrls;
}
