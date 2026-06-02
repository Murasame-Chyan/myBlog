package com.murasame.mapper;

import com.murasame.entity.Achievement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AchievementMapper {

    @Select("SELECT * FROM achievement ORDER BY id ASC")
    List<Achievement> findAll();

    @Select("SELECT * FROM achievement WHERE id = #{id}")
    Achievement findById(Integer id);
}
