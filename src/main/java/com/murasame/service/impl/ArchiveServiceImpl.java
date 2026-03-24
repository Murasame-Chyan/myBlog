package com.murasame.service.impl;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.BlogsBin;
import com.murasame.mapper.ArchiveMapper;
import com.murasame.service.ArchiveService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArchiveServiceImpl implements ArchiveService {
	@Resource
	ArchiveMapper archiveMapper;

	@Override
	public List<BlogBriefVO> getRecent5ArchivesBrief(){
		return archiveMapper.getRecent5ArchivesBrief();
	}

	@Override
	public long getTotalArchiveCount(){
		return archiveMapper.getTotalArchiveCount();
	}

	@Override
	public List<BlogBriefVO> getArchivesByPage(int page, int pageSize){
		int offset = (page - 1) * pageSize;
		return archiveMapper.getArchivesByPage(pageSize, offset);
	}

	@Override
	public BlogsBin getArchiveById(Long id){
		return archiveMapper.getArchiveById(id);
	}
}
