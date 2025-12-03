package com.murasame.service.impl;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.Blogs;
import com.murasame.mapper.IndexMapper;
import com.murasame.service.IndexService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
public class IndexServiceImpl implements IndexService {
	@Resource
	IndexMapper indexMapper;

	@Override
	public String getBlogById_toString(BigInteger id) {
		Blogs blog =  indexMapper.getBlogById(id);
		return blog.toString();
	}

	@Override
	public Blogs getBlogById(BigInteger id) {
		return indexMapper.getBlogById(id);
	}

	@Override
	public List<BlogBriefVO> getRecent5BlogsBrief(){
		return indexMapper.getRecent5BlogsBrief();
	}
}
