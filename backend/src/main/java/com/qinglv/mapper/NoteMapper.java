package com.qinglv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinglv.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    @Update("UPDATE ql_note SET like_count = COALESCE(like_count, 0) + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE ql_note SET like_count = GREATEST(COALESCE(like_count, 0) - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);
}
