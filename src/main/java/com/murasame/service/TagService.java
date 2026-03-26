package com.murasame.service;

import com.murasame.entity.Tag;

import java.util.List;

public interface TagService {
	List<Tag> getAllTags();

	Tag getTagById(Integer id);
}
