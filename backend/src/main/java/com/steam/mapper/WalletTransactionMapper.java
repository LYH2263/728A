package com.steam.mapper;

import com.steam.entity.WalletTransaction;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface WalletTransactionMapper {

    @Insert("INSERT INTO wallet_transactions (user_id, type, amount, balance_before, balance_after, order_no, description) " +
            "VALUES (#{userId}, #{type}, #{amount}, #{balanceBefore}, #{balanceAfter}, #{orderNo}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WalletTransaction transaction);

    @Select("SELECT * FROM wallet_transactions WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{size}")
    List<WalletTransaction> findByUserId(@Param("userId") Long userId,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    @Select("SELECT COUNT(*) FROM wallet_transactions WHERE user_id = #{userId}")
    long countByUserId(Long userId);

    @Select("<script>" +
            "SELECT * FROM wallet_transactions WHERE user_id = #{userId} " +
            "<if test='type != null and type != \"\"'> AND type = #{type} </if>" +
            "<if test='startDate != null'> AND created_at >= #{startDate} </if>" +
            "<if test='endDate != null'> AND created_at &lt;= #{endDate} </if>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<WalletTransaction> findByUserIdAndFilters(@Param("userId") Long userId,
                                                   @Param("type") String type,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("offset") int offset,
                                                   @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM wallet_transactions WHERE user_id = #{userId} " +
            "<if test='type != null and type != \"\"'> AND type = #{type} </if>" +
            "<if test='startDate != null'> AND created_at >= #{startDate} </if>" +
            "<if test='endDate != null'> AND created_at &lt;= #{endDate} </if>" +
            "</script>")
    long countByUserIdAndFilters(@Param("userId") Long userId,
                                 @Param("type") String type,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    @Select("SELECT type, SUM(amount) as total FROM wallet_transactions " +
            "WHERE user_id = #{userId} AND created_at >= #{startDate} AND created_at &lt;= #{endDate} " +
            "GROUP BY type")
    List<Map<String, Object>> getMonthlySummary(@Param("userId") Long userId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') as month, " +
            "SUM(CASE WHEN type IN ('RECHARGE', 'REFUND', 'GIFT') THEN amount ELSE 0 END) as income, " +
            "SUM(CASE WHEN type = 'PURCHASE' THEN ABS(amount) ELSE 0 END) as expense " +
            "FROM wallet_transactions " +
            "WHERE user_id = #{userId} AND created_at >= #{startDate} " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m') " +
            "ORDER BY month ASC")
    List<Map<String, Object>> getTrendData(@Param("userId") Long userId,
                                           @Param("startDate") LocalDateTime startDate);
}
