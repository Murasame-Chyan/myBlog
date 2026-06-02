package com.murasame.service;

import com.murasame.domain.dto.SignInResultDTO;
import com.murasame.domain.vo.SignInStatusVO;

public interface SignInService {

    /** 执行签到，返回签到结果 */
    SignInResultDTO signIn(Long userId);

    /** 获取当前用户签到状态 */
    SignInStatusVO getSignInStatus(Long userId);

    /** 小游戏补签（无门槛，+3经验） */
    SignInResultDTO makeupSignIn(Long userId);
}
