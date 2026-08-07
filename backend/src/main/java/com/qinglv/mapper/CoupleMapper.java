package com.qinglv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qinglv.entity.Couple;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 情侣空间Mapper
 */
@Mapper
public interface CoupleMapper extends BaseMapper<Couple> {

    /**
     * 在绑定事务中锁定邀请码对应的空间，防止并发请求占用同一个成员槽位。
     */
    @Select("SELECT * FROM ql_couple "
            + "WHERE invite_code = #{inviteCode} AND deleted = 0 "
            + "LIMIT 1 FOR UPDATE")
    Couple selectByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);
}
