package com.murasame.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class PermissionAspect {
    /**
     * 拦截所有带 @RequirePermission 的方法
     * "@annotation(xxx)" 表示：目标方法上有 xxx 注解的才拦截
     */
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                  RequirePermission requirePermission) throws Throwable {
        // 1. 获取当前登录用户的权限列表
        Set<String> userPermissions = getCurrentUserPermissions();
        // 2. 取出注解里要求的权限码
        String requiredPermission = requirePermission.value();
        // 3. 判断
        if (userPermissions.contains(requiredPermission)) {
            // 有权限 → 放行，继续执行 Controller 方法
            return joinPoint.proceed();
        }
        // 4. 无权限 → 抛异常，由全局异常处理器返回 403 JSON
        throw new PermissionDeniedException(
                "权限不足，需要权限码：" + requiredPermission
        );
    }

    /** 从 Spring Security 上下文取当前用户的权限码集合 */
    private Set<String> getCurrentUserPermissions() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || auth.getAuthorities() == null) {
            return Set.of();
        }

        return auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)  // 每个 authority 的字符串形式
                .collect(Collectors.toSet());
    }
}
