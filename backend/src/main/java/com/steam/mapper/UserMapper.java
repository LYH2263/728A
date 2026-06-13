package com.steam.mapper;

import com.steam.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(String email);
    
    @Insert("INSERT INTO users (username, password, email, nickname, avatar, balance, role, status) " +
            "VALUES (#{username}, #{password}, #{email}, #{nickname}, #{avatar}, #{balance}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    @Update("UPDATE users SET nickname = #{nickname}, email = #{email}, avatar = #{avatar}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(User user);
    
    @Update("UPDATE users SET balance = #{balance}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateBalance(@Param("id") Long id, @Param("balance") java.math.BigDecimal balance);

    @Select("SELECT * FROM users WHERE id = #{id} FOR UPDATE")
    User findByIdForUpdate(Long id);

    @Update("UPDATE users SET balance = balance + #{amount}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int addBalance(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);
    
    @Update("UPDATE users SET password = #{password}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Select("SELECT id, username, nickname, avatar, status FROM users " +
            "WHERE (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND status = 1 " +
            "ORDER BY username ASC " +
            "LIMIT #{offset}, #{size}")
    List<User> searchUsers(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM users " +
            "WHERE (username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND status = 1")
    Long countUsersByKeyword(String keyword);
}
