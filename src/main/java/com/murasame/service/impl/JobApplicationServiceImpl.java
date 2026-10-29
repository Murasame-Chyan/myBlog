package com.murasame.service.impl;

import com.murasame.domain.dto.JobApplicationCreateDTO;
import com.murasame.domain.dto.JobApplicationEventCreateDTO;
import com.murasame.domain.dto.JobApplicationQueryDTO;
import com.murasame.domain.dto.JobApplicationStatusChangeDTO;
import com.murasame.domain.dto.JobApplicationUpdateDTO;
import com.murasame.domain.vo.CountItemVO;
import com.murasame.domain.vo.JobApplicationBriefVO;
import com.murasame.domain.vo.JobApplicationDetailVO;
import com.murasame.domain.vo.JobApplicationEventVO;
import com.murasame.domain.vo.JobApplicationPageVO;
import com.murasame.domain.vo.JobApplicationStatsVO;
import com.murasame.entity.JobApplication;
import com.murasame.entity.JobApplicationEvent;
import com.murasame.mapper.JobApplicationEventMapper;
import com.murasame.mapper.JobApplicationMapper;
import com.murasame.service.JobApplicationService;
import com.murasame.util.CareerEnums;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 求职投递进度 Service 实现。
 *
 * 约定：
 * 1. 所有读写都带 userId，Mapper 负责 u_id 过滤，越权自然查不到；
 * 2. 状态变更只走 changeStatus，且必须在同一事务内写 status_change 事件；
 * 3. 派生字段（已投天数/状态停留/逾期/今日）在这里算，不落库；
 * 4. today()/now() 抽成 protected 方法，便于单测固定时间。
 */
@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_FOLLOW_UP_DAYS = 365;

    @Resource
    private JobApplicationMapper jobApplicationMapper;

    @Resource
    private JobApplicationEventMapper jobApplicationEventMapper;

    /** 供单测覆盖：默认系统日期 */
    protected LocalDate today() {
        return LocalDate.now();
    }

    /** 供单测覆盖：默认系统时间 */
    protected LocalDateTime now() {
        return LocalDateTime.now();
    }

    // ==================== 查询 ====================

    @Override
    public JobApplicationPageVO pageList(Long userId, JobApplicationQueryDTO query) {
        JobApplicationQueryDTO q = query == null ? new JobApplicationQueryDTO() : query;
        int page = normalizePage(q.getPage());
        int pageSize = normalizePageSize(q.getPageSize());
        int offset = (page - 1) * pageSize;

        String keyword = blankToNull(q.getKeyword());
        String status = normalizeStatusFilter(q.getStatus());
        String channel = normalizeChannelFilter(q.getChannel());
        boolean overdue = Boolean.TRUE.equals(q.getOverdue());
        String sortBy = normalizeSortBy(q.getSortBy());
        LocalDate today = today();

        List<JobApplication> rows = jobApplicationMapper.selectPage(
                userId, keyword, status, channel, overdue, sortBy, today, offset, pageSize);
        long total = jobApplicationMapper.countPage(userId, keyword, status, channel, overdue, today);

        JobApplicationPageVO vo = new JobApplicationPageVO();
        vo.setList(toBriefList(rows, today));
        vo.setTotal(total);
        vo.setPage(page);
        vo.setPageSize(pageSize);
        vo.setDueTodayList(toBriefList(
                jobApplicationMapper.selectDueToday(userId, today, CareerEnums.GROUP_LIST_LIMIT), today));
        vo.setOverdueList(toBriefList(
                jobApplicationMapper.selectOverdue(userId, today, CareerEnums.GROUP_LIST_LIMIT), today));
        return vo;
    }

    @Override
    public JobApplicationDetailVO detail(Long userId, Long id) {
        JobApplication app = jobApplicationMapper.selectByIdAndUser(id, userId);
        if (app == null) {
            return null;
        }
        LocalDate today = today();
        JobApplicationDetailVO vo = toDetail(app, today);

        JobApplicationEvent latest = jobApplicationEventMapper.selectLatestStatusChange(id, userId);
        LocalDateTime base = latest != null ? latest.getOccurredAt() : app.getCreatedAt();
        vo.setStatusDays(daysBetween(base, today));

        List<JobApplicationEvent> events = jobApplicationEventMapper.selectByApplication(id, userId);
        vo.setEvents(events == null ? new ArrayList<>() : events.stream()
                .map(this::toEventVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public List<JobApplicationEventVO> listEvents(Long userId, Long id) {
        if (jobApplicationMapper.selectByIdAndUser(id, userId) == null) {
            return null;
        }
        List<JobApplicationEvent> events = jobApplicationEventMapper.selectByApplication(id, userId);
        return events == null ? new ArrayList<>() : events.stream()
                .map(this::toEventVO).collect(Collectors.toList());
    }

    // ==================== 写入 ====================

    @Override
    @Transactional
    public Long create(Long userId, JobApplicationCreateDTO dto) {
        String status = blankToNull(dto.getStatus());
        if (status == null) {
            status = CareerEnums.DEFAULT_STATUS;
        }
        if (!CareerEnums.isValidStatus(status)) {
            throw new IllegalArgumentException("非法的状态值：" + status);
        }
        String priority = blankToNull(dto.getPriority());
        if (priority == null) {
            priority = CareerEnums.DEFAULT_PRIORITY;
        }
        if (!CareerEnums.isValidPriority(priority)) {
            throw new IllegalArgumentException("非法的优先级：" + priority);
        }
        String channel = blankToNull(dto.getChannel());
        if (!CareerEnums.isValidChannel(channel)) {
            throw new IllegalArgumentException("非法的渠道：" + dto.getChannel());
        }

        LocalDateTime now = now();
        JobApplication app = new JobApplication();
        app.setUId(userId);
        app.setCompany(blankToNull(dto.getCompany()));
        app.setPosition(blankToNull(dto.getPosition()));
        app.setChannel(channel);
        app.setLocation(blankToNull(dto.getLocation()));
        app.setJobUrl(blankToNull(dto.getJobUrl()));
        app.setAppliedAt(dto.getAppliedAt() != null ? dto.getAppliedAt() : today());
        app.setStatus(status);
        app.setPriority(priority);
        app.setSalaryRange(blankToNull(dto.getSalaryRange()));
        app.setResumeVersion(blankToNull(dto.getResumeVersion()));
        app.setContact(blankToNull(dto.getContact()));
        app.setNextAction(blankToNull(dto.getNextAction()));
        app.setFollowUpAt(dto.getFollowUpAt());
        app.setNotes(blankToNull(dto.getNotes()));
        app.setCreatedAt(now);
        app.setUpdatedAt(now);
        jobApplicationMapper.insert(app);

        // 新建只写一条 created 事件（初始状态记在事件正文里，不重复写 status_change）
        JobApplicationEvent event = new JobApplicationEvent();
        event.setApplicationId(app.getId());
        event.setUId(userId);
        event.setType(CareerEnums.EVENT_CREATED);
        event.setOccurredAt(now);
        event.setToStatus(status);
        event.setTitle("创建投递记录");
        event.setContent("初始状态：" + CareerEnums.statusLabel(status));
        event.setCreatedAt(now);
        jobApplicationEventMapper.insert(event);
        return app.getId();
    }

    @Override
    public boolean update(Long userId, Long id, JobApplicationUpdateDTO dto) {
        JobApplication app = jobApplicationMapper.selectByIdAndUser(id, userId);
        if (app == null) {
            return false;
        }
        String channel = blankToNull(dto.getChannel());
        if (!CareerEnums.isValidChannel(channel)) {
            throw new IllegalArgumentException("非法的渠道：" + dto.getChannel());
        }
        String priority = blankToNull(dto.getPriority());
        if (priority != null && !CareerEnums.isValidPriority(priority)) {
            throw new IllegalArgumentException("非法的优先级：" + priority);
        }
        app.setCompany(blankToNull(dto.getCompany()));
        app.setPosition(blankToNull(dto.getPosition()));
        app.setChannel(channel);
        app.setLocation(blankToNull(dto.getLocation()));
        app.setJobUrl(blankToNull(dto.getJobUrl()));
        app.setAppliedAt(dto.getAppliedAt() != null ? dto.getAppliedAt() : app.getAppliedAt());
        if (priority != null) {
            app.setPriority(priority);
        }
        app.setSalaryRange(blankToNull(dto.getSalaryRange()));
        app.setResumeVersion(blankToNull(dto.getResumeVersion()));
        app.setContact(blankToNull(dto.getContact()));
        app.setNextAction(blankToNull(dto.getNextAction()));
        app.setFollowUpAt(dto.getFollowUpAt());
        app.setNotes(blankToNull(dto.getNotes()));
        app.setUpdatedAt(now());
        return jobApplicationMapper.updateByIdAndUser(app) > 0;
    }

    @Override
    @Transactional
    public boolean changeStatus(Long userId, Long id, JobApplicationStatusChangeDTO dto) {
        String toStatus = blankToNull(dto.getToStatus());
        if (!CareerEnums.isValidStatus(toStatus)) {
            throw new IllegalArgumentException("非法的状态值：" + dto.getToStatus());
        }
        JobApplication app = jobApplicationMapper.selectByIdAndUser(id, userId);
        if (app == null) {
            return false;
        }
        String fromStatus = app.getStatus();
        if (toStatus.equals(fromStatus)) {
            // 幂等：状态未变化，不重复写事件
            return true;
        }
        LocalDateTime now = now();
        if (jobApplicationMapper.updateStatus(id, userId, toStatus, now) == 0) {
            return false;
        }
        JobApplicationEvent event = new JobApplicationEvent();
        event.setApplicationId(id);
        event.setUId(userId);
        event.setType(CareerEnums.EVENT_STATUS_CHANGE);
        event.setOccurredAt(now);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setTitle("状态变更：" + CareerEnums.statusLabel(fromStatus) + " → " + CareerEnums.statusLabel(toStatus));
        event.setContent(blankToNull(dto.getNote()));
        event.setCreatedAt(now);
        jobApplicationEventMapper.insert(event);
        return true;
    }

    @Override
    @Transactional
    public boolean addEvent(Long userId, Long id, JobApplicationEventCreateDTO dto) {
        String type = blankToNull(dto.getType());
        if (!CareerEnums.isManualEventType(type)) {
            throw new IllegalArgumentException("非法的事件类型：" + dto.getType());
        }
        JobApplication app = jobApplicationMapper.selectByIdAndUser(id, userId);
        if (app == null) {
            return false;
        }
        LocalDateTime now = now();
        JobApplicationEvent event = new JobApplicationEvent();
        event.setApplicationId(id);
        event.setUId(userId);
        event.setType(type);
        event.setOccurredAt(dto.getOccurredAt() != null ? dto.getOccurredAt() : now);
        event.setTitle(blankToNull(dto.getTitle()));
        event.setContent(blankToNull(dto.getContent()));
        event.setMeta(blankToNull(dto.getMeta()));
        event.setCreatedAt(now);
        jobApplicationEventMapper.insert(event);
        return true;
    }

    @Override
    @Transactional
    public LocalDate followUp(Long userId, Long id, Integer days) {
        int plusDays = days == null ? CareerEnums.DEFAULT_FOLLOW_UP_DAYS : days;
        if (plusDays < 1 || plusDays > MAX_FOLLOW_UP_DAYS) {
            throw new IllegalArgumentException("跟进天数需在 1-" + MAX_FOLLOW_UP_DAYS + " 之间");
        }
        JobApplication app = jobApplicationMapper.selectByIdAndUser(id, userId);
        if (app == null) {
            return null;
        }
        LocalDate target = today().plusDays(plusDays);
        LocalDateTime now = now();
        if (jobApplicationMapper.updateFollowUp(id, userId, target, now) == 0) {
            return null;
        }
        JobApplicationEvent event = new JobApplicationEvent();
        event.setApplicationId(id);
        event.setUId(userId);
        event.setType(CareerEnums.EVENT_FOLLOW_UP);
        event.setOccurredAt(now);
        event.setTitle("跟进提醒：+" + plusDays + " 天");
        event.setContent("下次跟进日期：" + target);
        event.setCreatedAt(now);
        jobApplicationEventMapper.insert(event);
        return target;
    }

    @Override
    public boolean softDelete(Long userId, Long id) {
        return jobApplicationMapper.softDelete(id, userId, now()) > 0;
    }

    @Override
    public boolean restore(Long userId, Long id) {
        return jobApplicationMapper.restore(id, userId, now()) > 0;
    }

    @Override
    public List<JobApplicationBriefVO> deletedList(Long userId) {
        LocalDate today = today();
        return toBriefList(jobApplicationMapper.selectDeleted(userId), today);
    }

    @Override
    public String exportCsv(Long userId) {
        List<JobApplication> rows = jobApplicationMapper.selectAllForExport(userId);
        String lineSeparator = System.lineSeparator();
        StringBuilder builder = new StringBuilder();
        builder.append("公司,岗位,渠道,城市,投递日期,状态,优先级,薪资区间,简历版本,联系方式,下一步,跟进日期,JD链接,备注,创建时间,更新时间")
                .append(lineSeparator);
        for (JobApplication app : rows) {
            builder.append(csv(app.getCompany())).append(',')
                    .append(csv(app.getPosition())).append(',')
                    .append(csv(CareerEnums.channelLabel(app.getChannel()))).append(',')
                    .append(csv(app.getLocation())).append(',')
                    .append(csv(app.getAppliedAt())).append(',')
                    .append(csv(CareerEnums.statusLabel(app.getStatus()))).append(',')
                    .append(csv(CareerEnums.priorityLabel(app.getPriority()))).append(',')
                    .append(csv(app.getSalaryRange())).append(',')
                    .append(csv(app.getResumeVersion())).append(',')
                    .append(csv(app.getContact())).append(',')
                    .append(csv(app.getNextAction())).append(',')
                    .append(csv(app.getFollowUpAt())).append(',')
                    .append(csv(app.getJobUrl())).append(',')
                    .append(csv(app.getNotes())).append(',')
                    .append(csv(app.getCreatedAt())).append(',')
                    .append(csv(app.getUpdatedAt()))
                    .append(lineSeparator);
        }
        return builder.toString();
    }

    /** CSV 字段转义：含逗号/引号/换行时用双引号包裹，内部双引号翻倍 */
    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        String quote = String.valueOf((char) 34);
        if (text.indexOf(',') >= 0 || text.indexOf(quote) >= 0
                || text.indexOf(10) >= 0 || text.indexOf(13) >= 0) {
            text = quote + text.replace(quote, quote + quote) + quote;
        }
        return text;
    }

    // ==================== 统计 ====================

    @Override
    public JobApplicationStatsVO stats(Long userId) {
        LocalDate today = today();
        Map<String, Long> statusCounts = toCountMap(jobApplicationMapper.countByStatus(userId));
        Map<String, Long> channelCounts = toCountMap(jobApplicationMapper.countByChannel(userId));

        JobApplicationStatsVO vo = new JobApplicationStatsVO();
        vo.setTotal(jobApplicationMapper.countTotal(userId));
        vo.setOverdue(jobApplicationMapper.countOverdue(userId, today));
        vo.setDueToday(jobApplicationMapper.countDueToday(userId, today));

        List<CountItemVO> byStatus = new ArrayList<>();
        for (Map.Entry<String, String> entry : CareerEnums.statusLabels().entrySet()) {
            byStatus.add(new CountItemVO(entry.getKey(), entry.getValue(),
                    statusCounts.getOrDefault(entry.getKey(), 0L)));
        }
        // 兜底：库里出现未知状态码时也要展示
        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            if (!CareerEnums.statusLabels().containsKey(entry.getKey())) {
                byStatus.add(new CountItemVO(entry.getKey(), entry.getKey(), entry.getValue()));
            }
        }
        vo.setByStatus(byStatus);

        List<CountItemVO> byChannel = new ArrayList<>();
        for (Map.Entry<String, String> entry : CareerEnums.channelLabels().entrySet()) {
            byChannel.add(new CountItemVO(entry.getKey(), entry.getValue(),
                    channelCounts.getOrDefault(entry.getKey(), 0L)));
        }
        vo.setByChannel(byChannel);
        return vo;
    }

    // ==================== 转换辅助 ====================

    private List<JobApplicationBriefVO> toBriefList(List<JobApplication> rows, LocalDate today) {
        if (rows == null) {
            return new ArrayList<>();
        }
        return rows.stream().map(row -> toBrief(row, today)).collect(Collectors.toList());
    }

    private JobApplicationBriefVO toBrief(JobApplication app, LocalDate today) {
        JobApplicationBriefVO vo = new JobApplicationBriefVO();
        vo.setId(app.getId());
        vo.setUId(app.getUId());
        vo.setCompany(app.getCompany());
        vo.setPosition(app.getPosition());
        vo.setChannel(app.getChannel());
        vo.setChannelLabel(CareerEnums.channelLabel(app.getChannel()));
        vo.setStatus(app.getStatus());
        vo.setStatusLabel(CareerEnums.statusLabel(app.getStatus()));
        vo.setPriority(app.getPriority());
        vo.setPriorityLabel(CareerEnums.priorityLabel(app.getPriority()));
        vo.setLocation(app.getLocation());
        vo.setJobUrl(app.getJobUrl());
        vo.setAppliedAt(app.getAppliedAt());
        vo.setFollowUpAt(app.getFollowUpAt());
        vo.setNextAction(app.getNextAction());
        vo.setCreatedAt(app.getCreatedAt());
        vo.setUpdatedAt(app.getUpdatedAt());
        vo.setDeletedAt(app.getDeletedAt());
        vo.setAppliedDays(daysBetween(app.getAppliedAt(), today));
        fillFollowUpFlags(vo, app, today);
        return vo;
    }

    private JobApplicationDetailVO toDetail(JobApplication app, LocalDate today) {
        JobApplicationDetailVO vo = new JobApplicationDetailVO();
        vo.setId(app.getId());
        vo.setUId(app.getUId());
        vo.setCompany(app.getCompany());
        vo.setPosition(app.getPosition());
        vo.setChannel(app.getChannel());
        vo.setChannelLabel(CareerEnums.channelLabel(app.getChannel()));
        vo.setLocation(app.getLocation());
        vo.setJobUrl(app.getJobUrl());
        vo.setAppliedAt(app.getAppliedAt());
        vo.setStatus(app.getStatus());
        vo.setStatusLabel(CareerEnums.statusLabel(app.getStatus()));
        vo.setPriority(app.getPriority());
        vo.setPriorityLabel(CareerEnums.priorityLabel(app.getPriority()));
        vo.setSalaryRange(app.getSalaryRange());
        vo.setResumeVersion(app.getResumeVersion());
        vo.setContact(app.getContact());
        vo.setNextAction(app.getNextAction());
        vo.setFollowUpAt(app.getFollowUpAt());
        vo.setNotes(app.getNotes());
        vo.setCreatedAt(app.getCreatedAt());
        vo.setUpdatedAt(app.getUpdatedAt());
        vo.setAppliedDays(daysBetween(app.getAppliedAt(), today));
        vo.setOverdue(isOverdue(app, today));
        vo.setDueToday(isDueToday(app, today));
        return vo;
    }

    private JobApplicationEventVO toEventVO(JobApplicationEvent event) {
        JobApplicationEventVO vo = new JobApplicationEventVO();
        vo.setId(event.getId());
        vo.setType(event.getType());
        vo.setTypeLabel(CareerEnums.eventTypeLabel(event.getType()));
        vo.setOccurredAt(event.getOccurredAt());
        vo.setFromStatus(event.getFromStatus());
        vo.setFromStatusLabel(CareerEnums.statusLabel(event.getFromStatus()));
        vo.setToStatus(event.getToStatus());
        vo.setToStatusLabel(CareerEnums.statusLabel(event.getToStatus()));
        vo.setTitle(event.getTitle());
        vo.setContent(event.getContent());
        vo.setMeta(event.getMeta());
        vo.setCreatedAt(event.getCreatedAt());
        return vo;
    }

    private void fillFollowUpFlags(JobApplicationBriefVO vo, JobApplication app, LocalDate today) {
        vo.setOverdue(isOverdue(app, today));
        vo.setDueToday(isDueToday(app, today));
    }

    /** 逾期：跟进日期已过、且状态不是结束类 */
    private boolean isOverdue(JobApplication app, LocalDate today) {
        return app.getFollowUpAt() != null && app.getFollowUpAt().isBefore(today) && isOpen(app);
    }

    /** 今日跟进：跟进日期正好是今天、且状态不是结束类 */
    private boolean isDueToday(JobApplication app, LocalDate today) {
        return app.getFollowUpAt() != null && app.getFollowUpAt().isEqual(today) && isOpen(app);
    }

    private boolean isOpen(JobApplication app) {
        return !CareerEnums.isClosedStatus(app.getStatus());
    }

    private int daysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(from, to);
        return days < 0 ? 0 : (int) days;
    }

    private int daysBetween(LocalDateTime from, LocalDate to) {
        return from == null ? 0 : daysBetween(from.toLocalDate(), to);
    }

    // ==================== 参数归一化 ====================

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static int normalizePage(Integer page) {
        return (page == null || page < 1) ? 1 : page;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static String normalizeStatusFilter(String status) {
        String value = blankToNull(status);
        return value != null && CareerEnums.isValidStatus(value) ? value : null;
    }

    private static String normalizeChannelFilter(String channel) {
        String value = blankToNull(channel);
        return value != null && CareerEnums.isValidChannel(value) ? value : null;
    }

    private static String normalizeSortBy(String sortBy) {
        String value = blankToNull(sortBy);
        return ("followUp".equals(value) || "updated".equals(value)) ? value : null;
    }

    /** 把 [{code, cnt}] 转成 code -> count（列名大小写不敏感，兼容不同驱动） */
    private static Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            String code = stringValue(row, "code");
            String count = stringValue(row, "cnt");
            if (code == null || code.isBlank()) {
                continue;
            }
            long value = 0L;
            if (count != null) {
                try {
                    value = Long.parseLong(count);
                } catch (NumberFormatException ignored) {
                    value = 0L;
                }
            }
            result.put(code, value);
        }
        return result;
    }

    private static String stringValue(Map<String, Object> row, String key) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue() == null ? null : String.valueOf(entry.getValue());
            }
        }
        return null;
    }
}