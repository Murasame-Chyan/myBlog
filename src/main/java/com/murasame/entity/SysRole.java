package com.murasame.entity;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysRole {
    private Long id;
    @Size(max = 50, message = "角色名不能超过50个字符")
    private String name;
    @Size(max = 50, message = "角色编码不能超过50个字符")
    private String code;
    private Integer dataScope;
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;
}
