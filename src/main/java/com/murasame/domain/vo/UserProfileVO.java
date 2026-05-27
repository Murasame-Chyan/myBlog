package com.murasame.domain.vo;

import com.murasame.entity.Users;
import lombok.Data;

@Data
public class UserProfileVO {
    private Long id;
    private String nickname;
    private String intro;
    private String avatar;
    private Integer level;
    private Integer gender;
    private Integer exp;
    private String githubUsername;
    private Integer followerCount;
    private Integer followingCount;

    public static UserProfileVO from(Users user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setIntro(user.getIntro());
        vo.setAvatar(user.getAvatar());
        vo.setLevel(user.getLevel());
        vo.setGender(user.getGender());
        vo.setExp(user.getExp());
        vo.setGithubUsername(user.getGithubUsername());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setFollowingCount(user.getFollowingCount());
        return vo;
    }
}
