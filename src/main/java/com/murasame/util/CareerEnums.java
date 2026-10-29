package com.murasame.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 求职模块枚举与中文标签表。
 * 约定：英文码入库（改展示文案不影响历史数据），中文标签只用于展示。
 */
public final class CareerEnums {

    /** 新建默认状态：已投递（记录时通常已经投出去了） */
    public static final String DEFAULT_STATUS = "applied";
    /** 新建默认优先级 */
    public static final String DEFAULT_PRIORITY = "medium";
    /** 默认跟进间隔（天） */
    public static final int DEFAULT_FOLLOW_UP_DAYS = 7;
    /** 置顶分组最多返回条数 */
    public static final int GROUP_LIST_LIMIT = 20;

    public static final String STATUS_TO_APPLY = "to_apply";
    public static final String STATUS_APPLIED = "applied";
    public static final String STATUS_COMMUNICATING = "communicating";
    public static final String STATUS_WRITTEN_TEST = "written_test";
    public static final String STATUS_INTERVIEWING = "interviewing";
    public static final String STATUS_OFFER = "offer";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_DROPPED = "dropped";
    public static final String STATUS_ARCHIVED = "archived";

    public static final String EVENT_CREATED = "created";
    public static final String EVENT_STATUS_CHANGE = "status_change";
    public static final String EVENT_NOTE = "note";
    public static final String EVENT_INTERVIEW = "interview";
    public static final String EVENT_FOLLOW_UP = "follow_up";

    /** 状态码 -> 中文标签（LinkedHashMap 保证前端下拉顺序） */
    private static final Map<String, String> STATUS_LABELS = new LinkedHashMap<>();
    /** 渠道码 -> 中文标签 */
    private static final Map<String, String> CHANNEL_LABELS = new LinkedHashMap<>();
    /** 优先级码 -> 中文标签 */
    private static final Map<String, String> PRIORITY_LABELS = new LinkedHashMap<>();
    /** 事件类型 -> 中文标签 */
    private static final Map<String, String> EVENT_TYPE_LABELS = new LinkedHashMap<>();
    /** 允许用户手动追加的事件类型（created/status_change 由系统内部写入） */
    private static final Set<String> MANUAL_EVENT_TYPES = Set.of(EVENT_NOTE, EVENT_INTERVIEW, EVENT_FOLLOW_UP);
    /** 结束类状态：不再需要跟进（逾期/今日跟进判断时排除） */
    private static final Set<String> CLOSED_STATUSES = Set.of(
            STATUS_OFFER, STATUS_REJECTED, STATUS_DROPPED, STATUS_ARCHIVED);

    static {
        STATUS_LABELS.put(STATUS_TO_APPLY, "待投递");
        STATUS_LABELS.put(STATUS_APPLIED, "已投递");
        STATUS_LABELS.put(STATUS_COMMUNICATING, "沟通中");
        STATUS_LABELS.put(STATUS_WRITTEN_TEST, "笔试");
        STATUS_LABELS.put(STATUS_INTERVIEWING, "面试中");
        STATUS_LABELS.put(STATUS_OFFER, "Offer");
        STATUS_LABELS.put(STATUS_REJECTED, "拒绝");
        STATUS_LABELS.put(STATUS_DROPPED, "放弃");
        STATUS_LABELS.put(STATUS_ARCHIVED, "归档");

        CHANNEL_LABELS.put("official", "官网");
        CHANNEL_LABELS.put("boss", "BOSS直聘");
        CHANNEL_LABELS.put("liepin", "猎聘");
        CHANNEL_LABELS.put("lagou", "拉勾");
        CHANNEL_LABELS.put("referral", "内推");
        CHANNEL_LABELS.put("email", "邮箱");
        CHANNEL_LABELS.put("other", "其他");

        PRIORITY_LABELS.put("high", "高");
        PRIORITY_LABELS.put("medium", "中");
        PRIORITY_LABELS.put("low", "低");

        EVENT_TYPE_LABELS.put(EVENT_CREATED, "创建");
        EVENT_TYPE_LABELS.put(EVENT_STATUS_CHANGE, "状态变更");
        EVENT_TYPE_LABELS.put(EVENT_NOTE, "备注");
        EVENT_TYPE_LABELS.put(EVENT_INTERVIEW, "面试");
        EVENT_TYPE_LABELS.put(EVENT_FOLLOW_UP, "跟进");
    }

    private CareerEnums() {
    }

    public static boolean isValidStatus(String code) {
        return code != null && STATUS_LABELS.containsKey(code);
    }

    public static String statusLabel(String code) {
        return code == null ? null : STATUS_LABELS.getOrDefault(code, code);
    }

    public static Map<String, String> statusLabels() {
        return Collections.unmodifiableMap(STATUS_LABELS);
    }

    public static boolean isClosedStatus(String code) {
        return code != null && CLOSED_STATUSES.contains(code);
    }

    public static boolean isValidChannel(String code) {
        return code != null && CHANNEL_LABELS.containsKey(code);
    }

    public static String channelLabel(String code) {
        return code == null ? null : CHANNEL_LABELS.getOrDefault(code, code);
    }

    public static Map<String, String> channelLabels() {
        return Collections.unmodifiableMap(CHANNEL_LABELS);
    }

    public static boolean isValidPriority(String code) {
        return code != null && PRIORITY_LABELS.containsKey(code);
    }

    public static String priorityLabel(String code) {
        return code == null ? null : PRIORITY_LABELS.getOrDefault(code, code);
    }

    public static Map<String, String> priorityLabels() {
        return Collections.unmodifiableMap(PRIORITY_LABELS);
    }

    public static String eventTypeLabel(String type) {
        return type == null ? null : EVENT_TYPE_LABELS.getOrDefault(type, type);
    }

    public static Map<String, String> eventTypeLabels() {
        return Collections.unmodifiableMap(EVENT_TYPE_LABELS);
    }

    public static boolean isValidEventType(String type) {
        return type != null && EVENT_TYPE_LABELS.containsKey(type);
    }

    /** 是否允许用户手动追加该类型事件 */
    public static boolean isManualEventType(String type) {
        return type != null && MANUAL_EVENT_TYPES.contains(type);
    }
}
