package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建/编辑笔记请求DTO
 */
@Data
public class NoteDTO {

    /**
     * 笔记标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100个字符")
    private String title;

    /**
     * 笔记内容
     */
    @NotBlank(message = "内容不能为空")
    private String content;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 心情标签
     */
    private String mood;

    /**
     * 天气
     */
    private String weather;

    /**
     * 是否置顶：0-否 1-是
     */
    private Integer isTop;

    /**
     * 记录日期；为空时由服务端使用当天日期。
     */
    private LocalDate noteDate;

    /**
     * 图片URL列表
     */
    private List<String> images;
}
