package com.murasame.service.impl;

import com.murasame.domain.vo.BlogBriefVO;
import com.murasame.mapper.IndexMapper;
import com.murasame.service.IndexService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexServiceImpl implements IndexService {
	@Resource
	IndexMapper indexMapper;

	@Override
	public List<BlogBriefVO> getRecent5BlogsBrief(){
		return indexMapper.getRecent5BlogsBrief();
	}
}
