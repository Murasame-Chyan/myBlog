package com.murasame.service;

import com.murasame.entity.SysAnnouncement;

import java.util.List;

public interface AnnouncementService {
    /** 分页查询公告（status 为 null 表示全部） */
    List<SysAnnouncement> listByPage(Integer status, int page, int pageSize);

    /** 统计总数 */
    long count(Integer status);

    SysAnnouncement getById(Long id);

    /** 创建公告（默认草稿） */
    SysAnnouncement create(String title, String content, Long publisherId);

    /** 编辑公告 */
    void update(Long id, String title, String content);

    /** 发布公告 */
    void publish(Long id);

    /** 撤回公告 */
    void withdraw(Long id);

    /** 删除公告 */
    void delete(Long id);
}
