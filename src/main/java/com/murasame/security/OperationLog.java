package com.murasame.security;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 标在 Controller 方法上，声明该操作属于哪个模块、做了什么动作。
 * 由 {@link OperationLogAspect} 拦截，方法执行成功后自动记录到 sys_operation_log 表。
 * <p>
 * 用法示例：
 * <pre>
 * {@code
 * @OperationLog(module = "用户管理", action = "封禁用户")
 * @PutMapping("/{id}/status")
 * public Map<String, Object> updateStatus(...) { ... }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)            // 只贴在方法上
@Retention(RetentionPolicy.RUNTIME)    // 运行时保留，AOP 反射读取
@Documented
public @interface OperationLog {

    /** 操作模块，如 "用户管理" */
    String module();

    /** 操作动作，如 "封禁用户" */
    String action();
}
