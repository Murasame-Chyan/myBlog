package com.murasame.security;

import java.lang.annotation.*;

// 自定义权限注解
@Target(ElementType.METHOD)             // 该注解适用于方法
@Retention(RetentionPolicy.RUNTIME)     // 运行时保留，AOP 反射读取
@Documented
public @interface RequirePermission {
    /** 权限码，如 "blog:delete" */
    String value();

    /** 多权限时用 AND 还是 OR，默认 AND（都要有） */
    Logical logical() default Logical.AND;

    enum Logical { AND, OR }
}
