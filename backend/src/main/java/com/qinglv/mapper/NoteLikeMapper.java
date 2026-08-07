package com.qinglv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinglv.entity.NoteLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记点赞 Mapper
 */
@Mapper
public interface NoteLikeMapper extends BaseMapper<NoteLike> {
}
