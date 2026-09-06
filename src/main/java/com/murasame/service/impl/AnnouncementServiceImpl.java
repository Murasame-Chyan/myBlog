package com.murasame.service.impl;

import com.murasame.entity.SysAnnouncement;
import com.murasame.mapper.AnnouncementMapper;
import com.murasame.service.AnnouncementService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    // 状态常量：避免魔法数字散落各处
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_WITHDRAWN = 2;

    private final AnnouncementMapper announcementMapper;

    public AnnouncementServiceImpl(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @Override
    public List<SysAnnouncement> listByPage(Integer status, int page, int pageSize) {
        // page 从 1 开始，offset 从 0 开始，所以 (page-1) * pageSize
        int offset = (page - 1) * pageSize;
        return announcementMapper.findAll(status, pageSize, offset);
    }

    @Override
    public long count(Integer status) {
        return announcementMapper.count(status);
    }

    @Override
    public SysAnnouncement getById(Long id) {
        return announcementMapper.findById(id);
    }

    @Override
    public SysAnnouncement create(String title, String content, Long publisherId) {
        SysAnnouncement announcement = new SysAnnouncement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setStatus(STATUS_DRAFT);   // 新建默认草稿
        announcement.setPublisherId(publisherId);
        announcementMapper.insert(announcement);
        return announcement;
    }

    @Override
    public void update(Long id, String title, String content) {
        SysAnnouncement announcement = new SysAnnouncement();
        announcement.setId(id);
        announcement.setTitle(title);
        announcement.setContent(content);
        announcementMapper.update(announcement);
    }

    @Override
    public void publish(Long id) {
        // 发布：状态改为已发布，并记录发布时间
        announcementMapper.updateStatus(id, STATUS_PUBLISHED, LocalDateTime.now());
    }

    @Override
    public void withdraw(Long id) {
        // 撤回：状态改为已撤回，发布时间置空
        announcementMapper.updateStatus(id, STATUS_WITHDRAWN, null);
    }

    @Override
    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }
}
