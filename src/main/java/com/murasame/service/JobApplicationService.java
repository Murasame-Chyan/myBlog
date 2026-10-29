package com.murasame.service;

import com.murasame.domain.dto.JobApplicationCreateDTO;
import com.murasame.domain.dto.JobApplicationEventCreateDTO;
import com.murasame.domain.dto.JobApplicationQueryDTO;
import com.murasame.domain.dto.JobApplicationStatusChangeDTO;
import com.murasame.domain.dto.JobApplicationUpdateDTO;
import com.murasame.domain.vo.JobApplicationBriefVO;
import com.murasame.domain.vo.JobApplicationDetailVO;
import com.murasame.domain.vo.JobApplicationEventVO;
import com.murasame.domain.vo.JobApplicationPageVO;
import com.murasame.domain.vo.JobApplicationStatsVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 求职投递进度 Service。
 * 所有方法都以当前登录用户 userId 作为第一参数，内部强制按 u_id 过滤（越权返回 null / false）。
 */
public interface JobApplicationService {

    /** 列表（含今日跟进/逾期跟进置顶分组） */
    JobApplicationPageVO pageList(Long userId, JobApplicationQueryDTO query);

    /** 详情（含时间线与派生字段）；不存在或越权返回 null */
    JobApplicationDetailVO detail(Long userId, Long id);

    /** 新建：返回新记录 id；枚举非法抛 IllegalArgumentException */
    Long create(Long userId, JobApplicationCreateDTO dto);

    /** 编辑字段（不含状态）；不存在或越权返回 false */
    boolean update(Long userId, Long id, JobApplicationUpdateDTO dto);

    /** 改状态：同一事务内写 status_change 事件 */
    boolean changeStatus(Long userId, Long id, JobApplicationStatusChangeDTO dto);

    /** 追加时间线事件（仅 note/interview/follow_up） */
    boolean addEvent(Long userId, Long id, JobApplicationEventCreateDTO dto);

    /** 跟进 +N 天：返回新的跟进日期；不存在或越权返回 null */
    LocalDate followUp(Long userId, Long id, Integer days);

    /** 软删除 */
    boolean softDelete(Long userId, Long id);

    /** 统计：总数/逾期/今日/状态分布/渠道分布 */
    JobApplicationStatsVO stats(Long userId);

    /** 时间线事件列表；主记录不存在或越权返回 null */
    List<JobApplicationEventVO> listEvents(Long userId, Long id);

    /** 导出 CSV 文本（不含 BOM，仅当前用户未删除记录；字段已做 CSV 转义） */
    String exportCsv(Long userId);

    /** 回收站列表（已软删除，按删除时间倒序） */
    List<JobApplicationBriefVO> deletedList(Long userId);

    /** 恢复软删除记录；不存在或越权返回 false */
    boolean restore(Long userId, Long id);
}
