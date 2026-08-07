package com.qinglv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinglv.entity.NoteComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 笔记评论 Mapper
 */
@Mapper
public interface NoteCommentMapper extends BaseMapper<NoteComment> {

    /**
     * 批量统计笔记评论数（仅未删除）
     */
    @Select("<script>" +
            "SELECT note_id AS noteId, COUNT(*) AS cnt FROM ql_note_comment " +
            "WHERE note_id IN " +
            "<foreach item='id' collection='noteIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND deleted = 0 GROUP BY note_id" +
            "</script>")
    List<Map<String, Object>> selectCountByNoteIds(@Param("noteIds") List<Long> noteIds);
}
