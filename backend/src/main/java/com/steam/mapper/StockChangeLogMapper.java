package com.steam.mapper;

import com.steam.entity.StockChangeLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockChangeLogMapper {

    @Insert("INSERT INTO stock_change_logs (game_id, admin_id, admin_username, stock_before, stock_after, change_type, remark) " +
            "VALUES (#{gameId}, #{adminId}, #{adminUsername}, #{stockBefore}, #{stockAfter}, #{changeType}, #{remark})")
    int insert(StockChangeLog log);

    @Select("SELECT * FROM stock_change_logs WHERE game_id = #{gameId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<StockChangeLog> findByGameId(@Param("gameId") Long gameId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM stock_change_logs WHERE game_id = #{gameId}")
    Long countByGameId(@Param("gameId") Long gameId);
}
