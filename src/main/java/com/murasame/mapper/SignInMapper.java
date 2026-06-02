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
}
