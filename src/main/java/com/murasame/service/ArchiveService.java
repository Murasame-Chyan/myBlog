package com.murasame.service;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.BlogsBin;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ArchiveService {
	List<BlogBriefVO> getRecent5ArchivesBrief(Long uId);
	long getTotalArchiveCount(Long uId);
	List<BlogBriefVO> getArchivesByPage(int page, int pageSize, Long uId);
	BlogsBin getArchiveById(Long id);
	List<BlogBriefVO> getArchivesByTagId(Integer tagId, Long uId);
}
