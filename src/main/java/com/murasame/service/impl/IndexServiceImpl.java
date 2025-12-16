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
	IndexMapper indexMapper;

	@Override
	public List<BlogBriefVO> getRecent5BlogsBrief(){
		List<BlogBriefVO> blogsBrief = indexMapper.getRecent5BlogsBrief();
		// 自定义工具类：转换干净html文本
		for(BlogBriefVO blogBriefVO : blogsBrief){
			blogBriefVO.setBrief(BlogHtmlUtil.toHtml(blogBriefVO.getBrief()));
		}
		return blogsBrief;
	}
}
