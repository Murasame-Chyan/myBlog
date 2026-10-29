package com.murasame.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 编辑投递记录的请求体。注意：不含 status，状态只能通过 /status 接口修改。
 */
@Data
public class JobApplicationUpdateDTO {

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
