package com.murasame.service;

import com.murasame.entity.Users;

import java.util.List;

public interface FollowService {
    void follow(Long followerId, Long followeeId);
    void unfollow(Long followerId, Long followeeId);
    boolean isFollowing(Long followerId, Long followeeId);
    List<Users> getFollowers(Long userId, int page, int pageSize);
    List<Users> getFollowing(Long userId, int page, int pageSize);
    int countFollowers(Long userId);
    int countFollowing(Long userId);
}
