package com.murasame.service.impl;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.BlogsBin;
import com.murasame.mapper.ArchiveMapper;
import com.murasame.service.ArchiveService;
import com.murasame.util.BlogHtmlUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArchiveServiceImpl implements ArchiveService {
	@Resource
	private ArchiveMapper archiveMapper;

	@Override
	public List<BlogBriefVO> getRecent5ArchivesBrief(Long uId){
		List<BlogBriefVO> list = archiveMapper.getRecent5ArchivesBrief(uId);
		BlogHtmlUtil.processBriefs(list);
		return list;
	}

	@Override
	public long getTotalArchiveCount(Long uId){
		return archiveMapper.getTotalArchiveCount(uId);
	}

	@Override
	public List<BlogBriefVO> getArchivesByPage(int page, int pageSize, Long uId){
		int offset = (page - 1) * pageSize;
		List<BlogBriefVO> list = archiveMapper.getArchivesByPage(uId, pageSize, offset);
		BlogHtmlUtil.processBriefs(list);
		return list;
	}

	@Override
	public BlogsBin getArchiveById(Long id){
		return archiveMapper.getArchiveById(id);
	}

	@Override
	public List<BlogBriefVO> getArchivesByTagId(Integer tagId, Long uId){
		List<BlogBriefVO> list = archiveMapper.getArchivesByTagId(uId, tagId);
		BlogHtmlUtil.processBriefs(list);
		return list;
	}
}
