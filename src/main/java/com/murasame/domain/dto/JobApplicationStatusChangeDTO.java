package com.murasame.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改状态请求体：toStatus 必填，note 可选（会写入 status_change 事件的正文）。
 */
@Data
public class JobApplicationStatusChangeDTO {

    @NotBlank(message = "目标状态不能为空")
    private String toStatus;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String note;
}
