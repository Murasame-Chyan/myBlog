package com.murasame.domain.dto;

import lombok.Data;

/**
 * 列表查询参数（GET 查询串绑定）。空字符串由 Service 归一化为 null。
 */
@Data
public class JobApplicationQueryDTO {

    private String keyword;
    private String status;
    private String channel;
    /** 只看逾期：1/true 生效 */
    private Boolean overdue;
    /** 排序：followUp / updated / 缺省按投递时间倒序 */
    private String sortBy;
    private Integer page = 1;
    private Integer pageSize = 10;
}
