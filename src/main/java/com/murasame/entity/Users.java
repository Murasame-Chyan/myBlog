package com.murasame.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Users {
	Long id;
	String nickname;
	String intro;
	String avatar;
	Integer level;
	String email;
	Integer gender;
	Integer exp;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	String liked_b_id;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	String password;
}
