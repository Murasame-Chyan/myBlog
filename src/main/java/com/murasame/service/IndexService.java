package com.murasame.service;

import com.murasame.domain.vo.BlogBriefVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface IndexService {
	List<BlogBriefVO> getRecent5BlogsBrief();    // 取最近5条博客简介
}
