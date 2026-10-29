package com.murasame.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 新建投递记录的请求体。status/priority 可选，缺省由 Service 补默认值。
 */
@Data
public class JobApplicationCreateDTO {

    @NotBlank(message = "公司不能为空")
    @Size(max = 100, message = "公司名不能超过100个字符")
    private String company;

    @NotBlank(message = "岗位不能为空")
    @Size(max = 100, message = "岗位名不能超过100个字符")
    private String position;

    @NotBlank(message = "渠道不能为空")
    private String channel;

    @Size(max = 50, message = "城市不能超过50个字符")
    private String location;

    @Size(max = 500, message = "JD 链接不能超过500个字符")
    private String jobUrl;

    @NotNull(message = "投递日期不能为空")
    private LocalDate appliedAt;

    /** 可选：to_apply / applied（缺省 applied） */
    private String status;

    /** 可选：high / medium / low（缺省 medium） */
    private String priority;

    @Size(max = 50, message = "薪资区间不能超过50个字符")
    private String salaryRange;

    @Size(max = 50, message = "简历版本不能超过50个字符")
    private String resumeVersion;

    @Size(max = 100, message = "联系方式不能超过100个字符")
    private String contact;

    @Size(max = 200, message = "下一步动作不能超过200个字符")
    private String nextAction;

    private LocalDate followUpAt;

    @Size(max = 2000, message = "备注不能超过2000个字符")
    private String notes;
}
