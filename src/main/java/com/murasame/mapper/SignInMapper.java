package com.murasame.mapper;

import com.murasame.entity.SignInRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SignInMapper {

    @Insert("INSERT INTO sign_in_record (user_id, sign_date, letter, base_exp, bonus_exp, total_exp, is_makeup) " +
            "VALUES (#{userId}, #{signDate}, #{letter}, #{baseExp}, #{bonusExp}, #{totalExp}, #{isMakeup})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SignInRecord record);

    @Select("SELECT * FROM sign_in_record WHERE user_id = #{userId} AND sign_date = #{signDate} LIMIT 1")
    SignInRecord findByUserIdAndDate(@Param("userId") Long userId, @Param("signDate") LocalDate signDate);

    @Select("SELECT * FROM sign_in_record WHERE user_id = #{userId} ORDER BY sign_date DESC LIMIT 1")
    SignInRecord findLatestByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sign_in_record WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    @Select("SELECT sign_date FROM sign_in_record WHERE user_id = #{userId} AND sign_date >= #{since} ORDER BY sign_date ASC")
    List<LocalDate> findSignDatesSince(@Param("userId") Long userId, @Param("since") LocalDate since);

    /** 管理端：全站签到记录分页（含用户昵称） */
    @Select("<script>" +
            "SELECT s.*, u.nickname AS nickname " +
            "FROM sign_in_record s " +
            "LEFT JOIN users u ON s.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='userId != null'>AND s.user_id = #{userId} </if>" +
            "ORDER BY s.sign_date DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<java.util.Map<String, Object>> findAllPage(@Param("userId") Long userId,
                                                     @Param("pageSize") int pageSize,
                                                     @Param("offset") int offset);

    /** 管理端：全站签到记录总数 */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sign_in_record WHERE 1=1 " +
            "<if test='userId != null'>AND user_id = #{userId} </if>" +
            "</script>")
    long countAll(@Param("userId") Long userId);
}
