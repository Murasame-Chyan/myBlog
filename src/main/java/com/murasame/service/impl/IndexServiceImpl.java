package com.murasame.service.impl;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.mapper.IndexMapper;
import com.murasame.service.IndexService;
import com.murasame.util.BlogHtmlUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexServiceImpl implements IndexService {
	@Resource
	private IndexMapper indexMapper;

	@Override
	public List<BlogBriefVO> getRecent5BlogsBrief(){
		List<BlogBriefVO> list = indexMapper.getRecent5BlogsBrief();
		BlogHtmlUtil.processBriefs(list);
		return list;
	}

	@Override
	public long getTotalBlogCount(){
		return indexMapper.getTotalBlogCount();
	}

	@Override
	public List<BlogBriefVO> getBlogsByPage(int page, int pageSize){
		int offset = (page - 1) * pageSize;
		List<BlogBriefVO> list = indexMapper.getBlogsByPage(pageSize, offset);
		BlogHtmlUtil.processBriefs(list);
		return list;
	}

	@Override
	public List<BlogBriefVO> getHotBlogs(){
		List<BlogBriefVO> list = indexMapper.getHotBlogs();
		BlogHtmlUtil.processBriefs(list);
		return list;
	}
}
