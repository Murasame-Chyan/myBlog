package com.murasame.service;

import com.murasame.domain.vo.BlogBriefVO;

import java.util.List;

public interface LikesService {

    void like(Long userId, Long blogId);

    void unlike(Long userId, Long blogId);

    boolean isLiked(Long userId, Long blogId);

    List<BlogBriefVO> getLikedBlogs(Long userId, int limit);
}
