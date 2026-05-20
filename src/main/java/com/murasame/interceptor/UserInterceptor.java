package com.murasame.interceptor;

import com.murasame.entity.Users;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String method = request.getMethod();
        String path = request.getRequestURI();

        if (!requiresAuth(method, path)) {
            return true;
        }

        Users currentUser = (Users) request.getSession().getAttribute("currentUser");
        if (currentUser == null) {
            // 硬编码JSON响应体：拦截器无法使用ReturnUtil，直接写JSON供前端AJAX统一解析
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\"}");
            return false;
        }

        return true;
    }

    // 仅拦截POST写操作：发布/更新/删除/点赞/取消点赞/恢复博客、发评论、上传头像、修改设置
    private boolean requiresAuth(String method, String path) {
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return path.startsWith("/blogs/publish")
                || path.startsWith("/blogs/update")
                || path.startsWith("/blogs/delete")
                || path.startsWith("/blogs/like")
                || path.startsWith("/blogs/unlike")
                || path.startsWith("/blogs/recover")
                || path.startsWith("/user/comment/add")
                || path.startsWith("/user/avatar/upload")
                || path.startsWith("/user/settings");
    }
}
