package com.murasame.domain.vo;

import lombok.Data;

/**
 * 每日数量（近 30 天投递趋势用）。
 */
@Data
public class DailyCountVO {
    private String date;
    private long count;

    public DailyCountVO() {
    }

    public DailyCountVO(String date, long count) {
        this.date = date;
        this.count = count;
    }
}
