package com.murasame.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class Users {
	Long id;
	@Size(max = 32, message = "昵称不能超过32个字符")
	String nickname;
	@Size(max = 255, message = "简介不能超过255个字符")
	String intro;
	String avatar;
	Integer level;
	@Email(message = "邮箱格式不正确")
	@Size(max = 255, message = "邮箱不能超过255个字符")
	String email;
	Integer gender;
	Integer exp;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@Size(min = 6, max = 255, message = "密码必须在6-255位之间")
	String password;
	@Size(max = 255, message = "GitHub用户名不能超过255个字符")
	String githubUsername;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	String githubToken;     // AES-256-GCM 加密存储，仅写入不解出到前端
	Integer followerCount;
	Integer followingCount;
}
