package com.murasame.service;

import com.murasame.domain.dto.JobApplicationCreateDTO;
import com.murasame.domain.dto.JobApplicationQueryDTO;
import com.murasame.domain.dto.JobApplicationStatusChangeDTO;
import com.murasame.domain.vo.JobApplicationDetailVO;
import com.murasame.domain.vo.JobApplicationPageVO;
import com.murasame.entity.JobApplication;
import com.murasame.entity.JobApplicationEvent;
import com.murasame.mapper.JobApplicationEventMapper;
import com.murasame.mapper.JobApplicationMapper;
import com.murasame.service.impl.JobApplicationServiceImpl;
import com.murasame.util.CareerEnums;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 求职模块 Service 纯单测（Mockito，不启动 Spring 上下文、不连数据库）。
 * 覆盖：状态变更写事件、非法状态拒绝、越权返回 null/false、逾期与今日跨月跨年、
 *      分页 offset 与参数归一化、软删除、新建默认值与 created 事件。
 */
class JobApplicationServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long APP_ID = 100L;
    /** 固定“今天”为 2027-01-02，用于跨年边界测试 */
    private static final LocalDate TODAY = LocalDate.of(2027, 1, 2);

    private JobApplicationMapper appMapper;
    private JobApplicationEventMapper eventMapper;
    private TestableService service;

    /** 覆盖 today()/now()，让时间相关断言可复现 */
    private static class TestableService extends JobApplicationServiceImpl {
        private LocalDate fixedToday;

        @Override
        protected LocalDate today() {
            return fixedToday;
        }

        @Override
        protected LocalDateTime now() {
            return fixedToday.atTime(10, 0);
        }
    }

    @BeforeEach
    void setUp() {
        appMapper = mock(JobApplicationMapper.class);
        eventMapper = mock(JobApplicationEventMapper.class);
        service = new TestableService();
        ReflectionTestUtils.setField(service, "jobApplicationMapper", appMapper);
        ReflectionTestUtils.setField(service, "jobApplicationEventMapper", eventMapper);
        service.fixedToday = TODAY;
    }

    // ==================== 1. 改状态：更新主表 + 写 status_change 事件 ====================

    @Test
    void changeStatus_writesStatusChangeEvent() {
        when(appMapper.selectByIdAndUser(APP_ID, USER_ID))
                .thenReturn(application("applied", LocalDate.of(2026, 12, 1), null));
        when(appMapper.updateStatus(eq(APP_ID), eq(USER_ID), eq("interviewing"), any()))
                .thenReturn(1);

        JobApplicationStatusChangeDTO dto = new JobApplicationStatusChangeDTO();
        dto.setToStatus("interviewing");
        dto.setNote("约一面");

        assertTrue(service.changeStatus(USER_ID, APP_ID, dto));

        ArgumentCaptor<JobApplicationEvent> captor = ArgumentCaptor.forClass(JobApplicationEvent.class);
        verify(eventMapper).insert(captor.capture());
        JobApplicationEvent event = captor.getValue();
        assertEquals(CareerEnums.EVENT_STATUS_CHANGE, event.getType());
        assertEquals("applied", event.getFromStatus());
        assertEquals("interviewing", event.getToStatus());
        assertEquals("约一面", event.getContent());
        assertEquals(APP_ID, event.getApplicationId());
        assertEquals(USER_ID, event.getUId());
        assertNotNull(event.getOccurredAt());
    }

    // ==================== 2. 非法状态：抛错且不写库 ====================

    @Test
    void changeStatus_invalidStatus_throwsAndWritesNothing() {
        JobApplicationStatusChangeDTO dto = new JobApplicationStatusChangeDTO();
        dto.setToStatus("不存在的状态");

        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(USER_ID, APP_ID, dto));
        verifyNoInteractions(appMapper, eventMapper);
    }

    // ==================== 3. 越权：查不到一律 null / false ====================

    @Test
    void accessOtherUsersApplication_returnsNullOrFalse() {
        when(appMapper.selectByIdAndUser(APP_ID, USER_ID)).thenReturn(null);

        assertNull(service.detail(USER_ID, APP_ID));
        assertNull(service.listEvents(USER_ID, APP_ID));
        assertFalse(service.update(USER_ID, APP_ID, updateDto()));
        assertFalse(service.changeStatus(USER_ID, APP_ID, statusDto("offer")));
        assertNull(service.followUp(USER_ID, APP_ID, 7));
    }

    // ==================== 4. 逾期/今日：跨月跨年边界 + 终态排除 ====================

    @Test
    void detail_overdueAndDueToday_acrossYearBoundary() {
        when(eventMapper.selectLatestStatusChange(APP_ID, USER_ID)).thenReturn(null);
        when(eventMapper.selectByApplication(APP_ID, USER_ID)).thenReturn(new ArrayList<>());

        // 跟进日期 2026-12-31，今天 2027-01-02 => 逾期；已投 2026-12-01 => 32 天
        when(appMapper.selectByIdAndUser(APP_ID, USER_ID))
                .thenReturn(application("applied", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)));
        JobApplicationDetailVO overdue = service.detail(USER_ID, APP_ID);
        assertTrue(overdue.getOverdue());
        assertFalse(overdue.getDueToday());
        assertEquals(Integer.valueOf(32), overdue.getAppliedDays());
        assertEquals(Integer.valueOf(32), overdue.getStatusDays());

        // 跟进日期正好是今天 => 今日跟进
        when(appMapper.selectByIdAndUser(APP_ID, USER_ID))
                .thenReturn(application("applied", LocalDate.of(2026, 12, 20), TODAY));
        JobApplicationDetailVO dueToday = service.detail(USER_ID, APP_ID);
        assertTrue(dueToday.getDueToday());
        assertFalse(dueToday.getOverdue());

        // 终态（offer）即使跟进日期已过，也不算逾期
        when(appMapper.selectByIdAndUser(APP_ID, USER_ID))
                .thenReturn(application("offer", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)));
        JobApplicationDetailVO closed = service.detail(USER_ID, APP_ID);
        assertFalse(closed.getOverdue());
        assertFalse(closed.getDueToday());
    }

    // ==================== 5. 分页 offset 与参数归一化 ====================

    @Test
    void pageList_computesOffsetAndNormalizesParams() {
        when(appMapper.selectPage(any(), any(), any(), any(), anyBoolean(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new ArrayList<>());
        when(appMapper.countPage(any(), any(), any(), any(), anyBoolean(), any())).thenReturn(25L);
        when(appMapper.selectDueToday(any(), any(), anyInt())).thenReturn(new ArrayList<>());
        when(appMapper.selectOverdue(any(), any(), anyInt())).thenReturn(new ArrayList<>());

        JobApplicationQueryDTO query = new JobApplicationQueryDTO();
        query.setPage(3);
        query.setPageSize(10);
        query.setKeyword("   ");
        query.setStatus("bad_status");

        JobApplicationPageVO page = service.pageList(USER_ID, query);

        // 空白 keyword / 非法 status 归一化为 null；offset=(3-1)*10=20
        verify(appMapper).selectPage(eq(USER_ID), isNull(), isNull(), isNull(), eq(false), eq("applied_at DESC"), eq(TODAY), eq(20), eq(10));
        assertEquals(25L, page.getTotal());
        assertEquals(3, page.getPage());
        assertEquals(10, page.getPageSize());

        // page<1 -> 1；pageSize>100 -> 100
        JobApplicationQueryDTO abnormal = new JobApplicationQueryDTO();
        abnormal.setPage(0);
        abnormal.setPageSize(999);
        service.pageList(USER_ID, abnormal);
        verify(appMapper).selectPage(eq(USER_ID), isNull(), isNull(), isNull(), eq(false), eq("applied_at DESC"), eq(TODAY), eq(0), eq(100));
    }

    // ==================== 6. 软删除：以 Mapper 影响行数为准 ====================

    @Test
    void softDelete_returnsMapperResult() {
        when(appMapper.softDelete(eq(APP_ID), eq(USER_ID), any())).thenReturn(1);
        assertTrue(service.softDelete(USER_ID, APP_ID));

        when(appMapper.softDelete(eq(APP_ID), eq(USER_ID), any())).thenReturn(0);
        assertFalse(service.softDelete(USER_ID, APP_ID));
    }

    // ==================== 7. 新建：默认状态/优先级 + created 事件 ====================

    @Test
    void create_usesDefaultStatusAndWritesCreatedEvent() {
        when(appMapper.insert(any(JobApplication.class))).thenAnswer(invocation -> {
            JobApplication saved = invocation.getArgument(0);
            saved.setId(APP_ID);
            return 1;
        });

        JobApplicationCreateDTO dto = new JobApplicationCreateDTO();
        dto.setCompany("字节跳动");
        dto.setPosition("后端开发实习生");
        dto.setChannel("official");
        dto.setAppliedAt(LocalDate.of(2026, 12, 20));

        assertEquals(APP_ID, service.create(USER_ID, dto));

        ArgumentCaptor<JobApplication> appCaptor = ArgumentCaptor.forClass(JobApplication.class);
        verify(appMapper).insert(appCaptor.capture());
        JobApplication saved = appCaptor.getValue();
        assertEquals(CareerEnums.DEFAULT_STATUS, saved.getStatus());
        assertEquals(CareerEnums.DEFAULT_PRIORITY, saved.getPriority());
        assertEquals(USER_ID, saved.getUId());

        ArgumentCaptor<JobApplicationEvent> eventCaptor = ArgumentCaptor.forClass(JobApplicationEvent.class);
        verify(eventMapper).insert(eventCaptor.capture());
        JobApplicationEvent event = eventCaptor.getValue();
        assertEquals(CareerEnums.EVENT_CREATED, event.getType());
        assertEquals(APP_ID, event.getApplicationId());
    }

    // ==================== 8. CSV 导出：表头 + 特殊字符转义 ====================

    @Test
    void exportCsv_containsHeaderAndEscapesSpecialChars() {
        String quote = String.valueOf((char) 34);
        JobApplication app = application("applied", LocalDate.of(2026, 12, 1), null);
        app.setCompany("测试,公司" + quote + "甲" + quote);
        app.setNotes("第一行" + System.lineSeparator() + "第二行");
        when(appMapper.selectAllForExport(USER_ID)).thenReturn(java.util.List.of(app));

        String csv = service.exportCsv(USER_ID);

        assertTrue(csv.startsWith("公司,岗位"));
        assertTrue(csv.contains(quote + "测试,公司" + quote + quote + "甲" + quote + quote + quote));
        assertTrue(csv.contains("第一行" + System.lineSeparator() + "第二行"));
        assertTrue(csv.endsWith(System.lineSeparator()));
    }

    // ==================== 9. 恢复软删除 ====================

    @Test
    void restore_returnsMapperResult() {
        when(appMapper.restore(eq(APP_ID), eq(USER_ID), any())).thenReturn(1);
        assertTrue(service.restore(USER_ID, APP_ID));

        when(appMapper.restore(eq(APP_ID), eq(USER_ID), any())).thenReturn(0);
        assertFalse(service.restore(USER_ID, APP_ID));
    }

    // ==================== 10. 回收站列表：带删除时间 ====================

    @Test
    void deletedList_mapsDeletedAt() {
        JobApplication app = application("applied", LocalDate.of(2026, 12, 1), null);
        app.setDeletedAt(LocalDateTime.of(2026, 12, 20, 10, 30));
        when(appMapper.selectDeleted(USER_ID)).thenReturn(java.util.List.of(app));

        java.util.List<com.murasame.domain.vo.JobApplicationBriefVO> list = service.deletedList(USER_ID);

        assertEquals(1, list.size());
        assertEquals(app.getDeletedAt(), list.get(0).getDeletedAt());
        assertEquals(app.getCompany(), list.get(0).getCompany());
    }

    // ==================== 测试数据构造 ====================

    private JobApplication application(String status, LocalDate appliedAt, LocalDate followUpAt) {
        JobApplication app = new JobApplication();
        app.setId(APP_ID);
        app.setUId(USER_ID);
        app.setCompany("字节跳动");
        app.setPosition("后端开发");
        app.setChannel("referral");
        app.setPriority("medium");
        app.setStatus(status);
        app.setAppliedAt(appliedAt);
        app.setFollowUpAt(followUpAt);
        app.setCreatedAt(LocalDateTime.of(2026, 12, 1, 9, 0));
        return app;
    }

    private JobApplicationStatusChangeDTO statusDto(String toStatus) {
        JobApplicationStatusChangeDTO dto = new JobApplicationStatusChangeDTO();
        dto.setToStatus(toStatus);
        return dto;
    }

    private com.murasame.domain.dto.JobApplicationUpdateDTO updateDto() {
        com.murasame.domain.dto.JobApplicationUpdateDTO dto =
                new com.murasame.domain.dto.JobApplicationUpdateDTO();
        dto.setCompany("字节跳动");
        dto.setPosition("后端开发");
        dto.setChannel("official");
        dto.setAppliedAt(LocalDate.of(2026, 12, 1));
        return dto;
    }
}