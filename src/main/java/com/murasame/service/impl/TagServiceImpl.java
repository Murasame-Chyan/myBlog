package com.murasame.service.impl;

import com.murasame.entity.Tag;
import com.murasame.mapper.TagMapper;
import com.murasame.service.TagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {
	@Resource
	private TagMapper tagMapper;

	@Override
	public List<Tag> getAllTags() {
		return tagMapper.getAllTags();
	}

	@Override
	public Tag getTagById(Integer id) {
		return tagMapper.getTagById(id);
	}
}
