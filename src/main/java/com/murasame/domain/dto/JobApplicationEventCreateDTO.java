package com.murasame.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 追加时间线事件（仅允许 note / interview / follow_up 三种类型）。
 */
@Data
public class JobApplicationEventCreateDTO {

    @NotBlank(message = "事件类型不能为空")
    private String type;

    @NotBlank(message = "事件标题不能为空")
    @Size(max = 200, message = "事件标题不能超过200个字符")
    private String title;

    @Size(max = 2000, message = "事件内容不能超过2000个字符")
    private String content;

    /** 可选，缺省为当前时间 */
    private LocalDateTime occurredAt;

    @Size(max = 500, message = "扩展信息不能超过500个字符")
    private String meta;
}
