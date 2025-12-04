package com.murasame.service;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.entity.Blogs;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
public interface IndexService {
	public List<BlogBriefVO> getRecent5BlogsBrief();    // 取最近5条博客简介
}
