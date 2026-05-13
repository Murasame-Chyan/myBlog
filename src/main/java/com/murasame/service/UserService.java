package com.murasame.service;

import com.murasame.entity.Users;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
	Users getUserById(Long id);

	String getNicknameById(Long id);

	int updateAvatar(Long userId, String avatarUrl);

	Users register(String email, String nickname, String password);

	Users login(String email, String password);

	Users updateProfile(Users user);

	int calculateLevel(int exp);
}