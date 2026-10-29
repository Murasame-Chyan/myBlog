package com.murasame.domain.vo;

import lombok.Data;

/**
 * 通用分组统计项：code + 中文标签 + 数量。
 */
@Data
public class CountItemVO {
    private String code;
    private String label;
    private long count;

    public CountItemVO() {
    }

    public CountItemVO(String code, String label, long count) {
        this.code = code;
        this.label = label;
        this.count = count;
    }
}
