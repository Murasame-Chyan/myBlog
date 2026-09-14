package com.murasame.mapper;

import com.murasame.entity.Achievement;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AchievementMapper {

    @Select("SELECT * FROM achievement ORDER BY id ASC")
    List<Achievement> findAll();

    @Select("SELECT * FROM achievement WHERE id = #{id}")
    Achievement findById(@Param("id") Integer id);

    @Insert("INSERT INTO achievement (name, icon, full_image) VALUES (#{name}, #{icon}, #{fullImage})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Achievement achievement);

    @Update("UPDATE achievement SET name = #{name}, icon = #{icon}, full_image = #{fullImage} WHERE id = #{id}")
    int update(Achievement achievement);

    @Delete("DELETE FROM achievement WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);
}
