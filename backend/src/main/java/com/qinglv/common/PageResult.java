package com.qinglv.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应体
 */
@Data
public class PageResult<T> implements Serializable {

    /**
     * 当前页码
     */
    private Long pageNum;

    /**
     * 每页大小
     */
    private Long pageSize;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 数据列表
     */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(Long pageNum, Long pageSize, Long total, Long pages, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = pages;
        this.records = records;
    }

    public static <T> PageResult<T> of(Long pageNum, Long pageSize, Long total, Long pages, List<T> records) {
        return new PageResult<>(pageNum, pageSize, total, pages, records);
    }
}
