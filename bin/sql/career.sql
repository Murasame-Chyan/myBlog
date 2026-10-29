-- ============================================
-- 求职进度模块（MovingOn）建表脚本
-- 执行方式：IntelliJ IDEA 的 Database 控制台连接 myblog 库后整段执行
-- 版本：1.0（与 src/main/java/com/murasame/entity 下实体对应）
-- ============================================

-- 1) 投递主记录
CREATE TABLE IF NOT EXISTS job_application
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    u_id           BIGINT       NOT NULL COMMENT '用户ID',
    company        VARCHAR(100) NOT NULL COMMENT '公司名',
    `position`       VARCHAR(100) NOT NULL COMMENT '岗位名称',
    channel        VARCHAR(20)  NOT NULL DEFAULT 'other' COMMENT '渠道码：official/boss/liepin/lagou/referral/email/other',
    location       VARCHAR(50)  NULL COMMENT '城市',
    job_url        VARCHAR(500) NULL COMMENT 'JD 链接',
    applied_at     DATE         NOT NULL COMMENT '投递日期',
    `status`         VARCHAR(20)  NOT NULL DEFAULT 'applied' COMMENT '状态码：to_apply/applied/communicating/written_test/interviewing/offer/rejected/dropped/archived',
    priority       VARCHAR(10)  NOT NULL DEFAULT 'medium' COMMENT '优先级：high/medium/low',
    salary_range   VARCHAR(50)  NULL COMMENT '薪资区间（自由文本）',
    resume_version VARCHAR(50)  NULL COMMENT '使用的简历版本',
    contact        VARCHAR(100) NULL COMMENT '联系人/联系方式',
    next_action    VARCHAR(200) NULL COMMENT '下一步动作',
    follow_up_at   DATE         NULL COMMENT '下次跟进日期',
    notes          TEXT         NULL COMMENT '备注',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at     DATETIME     NULL COMMENT '软删除时间，NULL=未删除',
    INDEX idx_u_deleted (u_id, deleted_at),
    INDEX idx_u_status (u_id, `status`),
    INDEX idx_u_applied (u_id, applied_at),
    INDEX idx_u_followup (u_id, follow_up_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='求职投递主记录';

-- 2) 时间线事件（只追加，不编辑；主记录软删后事件保留）
CREATE TABLE IF NOT EXISTS job_application_event
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    application_id BIGINT       NOT NULL COMMENT '所属投递记录ID',
    u_id           BIGINT       NOT NULL COMMENT '冗余用户ID，便于归属校验',
    `type`           VARCHAR(20)  NOT NULL COMMENT '事件类型：created/status_change/note/interview/follow_up',
    occurred_at    DATETIME     NOT NULL COMMENT '事件发生时间',
    from_status    VARCHAR(20)  NULL COMMENT '变更前状态码',
    to_status      VARCHAR(20)  NULL COMMENT '变更后状态码',
    title          VARCHAR(200) NOT NULL COMMENT '事件标题',
    content        TEXT         NULL COMMENT '事件正文',
    meta           TEXT         NULL COMMENT '扩展信息（面试轮次/面试官等，MVP 用文本）',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_app_occurred (application_id, occurred_at),
    INDEX idx_u_occurred (u_id, occurred_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='求职投递时间线事件';
