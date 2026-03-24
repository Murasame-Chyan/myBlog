package com.murasame.mapper;

import com.murasame.entity.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
	@Select("SELECT * FROM users WHERE id=#{id}")
	Users getUserById(@Param("id") Long id);

	@Select("SELECT nickname FROM users WHERE id=#{id}")
	String getNicknameById(@Param("id") Long id);
}
