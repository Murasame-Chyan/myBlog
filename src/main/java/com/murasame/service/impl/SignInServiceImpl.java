package com.murasame.service.impl;

import com.murasame.domain.dto.SignInResultDTO;
import com.murasame.domain.vo.SignInStatusVO;
import com.murasame.entity.SignInRecord;
import com.murasame.entity.Users;
import com.murasame.mapper.SignInMapper;
import com.murasame.service.SignInService;
import com.murasame.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SignInServiceImpl implements SignInService {

    // 字母组别到基础经验值的映射
    private static final int[] LETTER_GROUP_EXP = {0, 0, 0, 3, 4, 5, 6, 7, 8, 9};
    // 每个字母所属的组别(经验值)
    private static final int[] LETTER_TO_GROUP = buildLetterGroups();

    private static int[] buildLetterGroups() {
        // A=0, B=1, ..., Z=25
        int[] groups = new int[26];
        // 3: AB, 4: CD, 5: EFG, 6: HIJK, 7: LMNO, 8: PQRST, 9: UVWXYZ
        String[] grpStr = {"", "", "", "AB", "CD", "EFG", "HIJK", "LMNO", "PQRST", "UVWXYZ"};
        for (int g = 3; g <= 9; g++) {
            for (char c : grpStr[g].toCharArray()) {
                groups[c - 'A'] = g;
            }
        }
        return groups;
    }

    @Resource
    private SignInMapper signInMapper;

    @Resource
    private UserService userService;

    @Override
    @Transactional
    public SignInResultDTO signIn(Long userId) {
        LocalDate today = LocalDate.now();
        // 防重：检查今天是否已签到
        SignInRecord exist = signInMapper.findByUserIdAndDate(userId, today);
        if (exist != null) {
            throw new IllegalStateException("今天已经签到过了");
        }

        // 计算连续签到天数
        int consecutiveDays = calcConsecutiveDays(userId, today);

        // 随机抽取幸运字母
        char letter = (char) ('A' + ThreadLocalRandom.current().nextInt(26));
        int group = LETTER_TO_GROUP[letter - 'A'];
        int baseExp = group;  // 3-9

        // 计算加成
        Users user = userService.getUserById(userId);
        int bonusExp = calcBonus(consecutiveDays, baseExp, user.getLevel());

        int totalExp = baseExp + bonusExp;

        // 写入签到记录
        SignInRecord record = new SignInRecord();
        record.setUserId(userId);
        record.setSignDate(today);
        record.setLetter(String.valueOf(letter));
        record.setBaseExp(baseExp);
        record.setBonusExp(bonusExp);
        record.setTotalExp(totalExp);
        record.setIsMakeup(false);

        try {
            signInMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("今天已经签到过了");
        }

        // 增加用户经验
        int oldLevel = user.getLevel() != null ? user.getLevel() : 1;
        int newExp = userService.addExp(userId, totalExp);
        int newLevel = userService.calculateLevel(newExp);

        SignInResultDTO result = new SignInResultDTO();
        result.setLetter(String.valueOf(letter));
        result.setLetterGroup(group);
        result.setBaseExp(baseExp);
        result.setBonusExp(bonusExp);
        result.setTotalExp(totalExp);
        result.setNewExp(newExp);
        result.setNewLevel(newLevel);
        result.setLeveledUp(newLevel > oldLevel);
        result.setConsecutiveDays(consecutiveDays);
        result.setMakeup(false);
        return result;
    }

    @Override
    public SignInStatusVO getSignInStatus(Long userId) {
        if (userId == null) {
            SignInStatusVO vo = new SignInStatusVO();
            vo.setLoggedIn(false);
            return vo;
        }

        LocalDate today = LocalDate.now();
        SignInRecord todayRecord = signInMapper.findByUserIdAndDate(userId, today);

        SignInRecord latest = signInMapper.findLatestByUserId(userId);
        int consecutiveDays = calcConsecutiveDays(userId, today);

        // 计算遗漏天数（最近7天内）
        LocalDate since = today.minusDays(7);
        List<LocalDate> signedDates = signInMapper.findSignDatesSince(userId, since);
        List<String> missedDays = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = since.plusDays(i);
            if (d.isBefore(today) && !signedDates.contains(d)) {
                missedDays.add(d.toString());
            }
        }

        int exp = userService.getExp(userId);
        int level = userService.calculateLevel(exp);
        int nextLevelExp = userService.getExpForNextLevel(exp);
        int currentLevelExp = userService.getCurrentLevelExp(exp);

        SignInStatusVO vo = new SignInStatusVO();
        vo.setLoggedIn(true);
        vo.setSignedToday(todayRecord != null);
        vo.setConsecutiveDays(consecutiveDays);
        vo.setMissedDays(missedDays);
        vo.setCanMakeup(!missedDays.isEmpty());
        vo.setTodayLetter(todayRecord != null ? todayRecord.getLetter() : null);
        vo.setTotalSignIns(signInMapper.countByUserId(userId));
        vo.setCurrentExp(exp);
        vo.setCurrentLevel(level);
        vo.setExpForNextLevel(nextLevelExp > 0 ? nextLevelExp : 999999);
        vo.setExpProgressInLevel(exp - currentLevelExp);
        vo.setCurrentLevelExp(currentLevelExp);
        vo.setMaxLevel(level >= 6);
        return vo;
    }

    @Override
    @Transactional
    public SignInResultDTO makeupSignIn(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(7);
        List<LocalDate> signedDates = signInMapper.findSignDatesSince(userId, since);

        // 找到最早遗漏日期
        LocalDate makeupDate = null;
        for (int i = 0; i < 7; i++) {
            LocalDate d = since.plusDays(i);
            if (d.isBefore(today) && !signedDates.contains(d)) {
                makeupDate = d;
                break;
            }
        }

        if (makeupDate == null) {
            SignInResultDTO empty = new SignInResultDTO();
            empty.setMakeup(false);
            return empty;
        }

        // 检查该日期是否已被补签
        SignInRecord exist = signInMapper.findByUserIdAndDate(userId, makeupDate);
        if (exist != null) {
            // 已被补签，尝试下一个缺失日
            for (int i = 1; i < 7; i++) {
                LocalDate d = since.plusDays(i);
                if (d.isBefore(today) && !signedDates.contains(d)) {
                    SignInRecord check = signInMapper.findByUserIdAndDate(userId, d);
                    if (check == null) {
                        makeupDate = d;
                        break;
                    }
                }
            }
            if (makeupDate == null || makeupDate.equals(since.plusDays(0)) && signedDates.contains(makeupDate)) {
                SignInResultDTO empty = new SignInResultDTO();
                empty.setMakeup(false);
                return empty;
            }
        }

        int makeupExp = 3;
        // 补签字母标为 '-'
        SignInRecord record = new SignInRecord();
        record.setUserId(userId);
        record.setSignDate(makeupDate);
        record.setLetter("-");
        record.setBaseExp(makeupExp);
        record.setBonusExp(0);
        record.setTotalExp(makeupExp);
        record.setIsMakeup(true);

        try {
            signInMapper.insert(record);
        } catch (DuplicateKeyException e) {
            SignInResultDTO empty = new SignInResultDTO();
            empty.setMakeup(false);
            return empty;
        }

        Users user = userService.getUserById(userId);
        int oldLevel = user.getLevel() != null ? user.getLevel() : 1;
        int newExp = userService.addExp(userId, makeupExp);
        int newLevel = userService.calculateLevel(newExp);

        SignInResultDTO result = new SignInResultDTO();
        result.setLetter("-");
        result.setLetterGroup(0);
        result.setBaseExp(makeupExp);
        result.setBonusExp(0);
        result.setTotalExp(makeupExp);
        result.setNewExp(newExp);
        result.setNewLevel(newLevel);
        result.setLeveledUp(newLevel > oldLevel);
        result.setConsecutiveDays(calcConsecutiveDays(userId, today));
        result.setMakeup(true);
        return result;
    }

    /** 计算连续签到天数：从昨天起向前连续的天数 + 1(今天) */
    private int calcConsecutiveDays(Long userId, LocalDate today) {
        SignInRecord latest = signInMapper.findLatestByUserId(userId);
        if (latest == null) return 1;

        LocalDate lastDate = latest.getSignDate();
        if (lastDate.equals(today)) {
            // 已签到的情况，从昨天往前数
            LocalDate cursor = today;
            int count = 0;
            for (int i = 1; i <= 365; i++) {
                LocalDate d = today.minusDays(i);
                SignInRecord r = signInMapper.findByUserIdAndDate(userId, d);
                if (r != null) {
                    count++;
                } else {
                    break;
                }
            }
            return count > 0 ? count + 1 : 1;
        } else if (lastDate.equals(today.minusDays(1))) {
            // 昨天签了，从昨天开始往前数
            int count = 1;
            for (int i = 2; i <= 365; i++) {
                LocalDate d = today.minusDays(i);
                SignInRecord r = signInMapper.findByUserIdAndDate(userId, d);
                if (r != null) {
                    count++;
                } else {
                    break;
                }
            }
            return count + 1;  // +1 代表今天即将签到
        } else {
            return 1;  // 断签
        }
    }

    /** 计算连击加成 */
    private int calcBonus(int consecutiveDays, int baseExp, int currentLevel) {
        int bonus = 0;
        if (consecutiveDays == 2) {
            bonus = baseExp;
        } else if (consecutiveDays >= 3) {
            bonus = baseExp * 2 + baseExp * ThreadLocalRandom.current().nextInt(3, 7); // 3-6
        }
        // 满级加成
        if (currentLevel >= 6) {
            bonus += 3 * ThreadLocalRandom.current().nextInt(1, 7); // 1-6
        }
        return bonus;
    }
}
