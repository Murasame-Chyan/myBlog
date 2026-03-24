package com.murasame.service;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.BlogsBin;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ArchiveService {
	List<BlogBriefVO> getRecent5ArchivesBrief();
	long getTotalArchiveCount();
	List<BlogBriefVO> getArchivesByPage(int page, int pageSize);
	BlogsBin getArchiveById(Long id);
}
