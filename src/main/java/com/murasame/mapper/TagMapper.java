package com.murasame.mapper;

import com.murasame.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper {
	@Select("SELECT * FROM tag")
	List<Tag> getAllTags();

	@Select("SELECT * FROM tag WHERE id=#{id}")
	Tag getTagById(@Param("id") Integer id);
}
