package com.murasame.entity;

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
}
