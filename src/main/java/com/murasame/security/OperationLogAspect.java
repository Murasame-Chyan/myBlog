package com.murasame.security;

import com.murasame.entity.Achievement;
import com.murasame.entity.Blogs;
import com.murasame.entity.Comments;
import com.murasame.entity.SysAnnouncement;
import com.murasame.entity.SysOperationLog;
import com.murasame.entity.SysRole;
import com.murasame.entity.Tag;
import com.murasame.entity.Users;
import com.murasame.mapper.AchievementMapper;
import com.murasame.mapper.AnnouncementMapper;
import com.murasame.mapper.BlogMapper;
import com.murasame.mapper.CommentMapper;
import com.murasame.mapper.OperationLogMapper;
import com.murasame.mapper.RoleMapper;
import com.murasame.mapper.TagMapper;
import com.murasame.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Parameter;

/**
 * 操作日志切面
 * <p>
 * 拦截所有带 {@link OperationLog} 注解的方法，方法执行成功后自动写操作日志。
 * 与 {@link PermissionAspect} 的区别：
 * <ul>
 *   <li>PermissionAspect：方法执行<strong>前</strong>校验权限，没权限抛异常（安全边界）</li>
 *   <li>OperationLogAspect：方法执行<strong>后</strong>记录日志，日志失败不影响业务（审计记录）</li>
 * </ul>
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;
    private final BlogMapper blogMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final RoleMapper roleMapper;
    private final AnnouncementMapper announcementMapper;
    private final AchievementMapper achievementMapper;

    public OperationLogAspect(OperationLogMapper operationLogMapper,
                              UserMapper userMapper,
                              BlogMapper blogMapper,
                              TagMapper tagMapper,
                              CommentMapper commentMapper,
                              RoleMapper roleMapper,
                              AnnouncementMapper announcementMapper,
                              AchievementMapper achievementMapper) {
        this.operationLogMapper = operationLogMapper;
        this.userMapper = userMapper;
        this.blogMapper = blogMapper;
        this.tagMapper = tagMapper;
        this.commentMapper = commentMapper;
        this.roleMapper = roleMapper;
        this.announcementMapper = announcementMapper;
        this.achievementMapper = achievementMapper;
    }

    /**
     * 环绕通知：拦截带 @OperationLog 的方法
     * "@annotation(operationLog)" 的变量名 operationLog 必须与方法参数名一致
     */
    @Around("@annotation(operationLog)")
    public Object recordLog(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        // ① 方法执行前，先提取目标对象信息。
        //    关键：必须在 proceed() 之前查名称，因为删除/下架操作执行后对象就不存在了
        Long targetId = extractTargetId(joinPoint);
        String targetName = resolveTargetName(operationLog.module(), targetId);

        // ② 执行目标方法（抛异常则向上抛，不记日志）
        Object result = joinPoint.proceed();

        // ③ 方法成功后记录日志
        try {
            SysOperationLog sysLog = new SysOperationLog();

            // 当前登录用户（JwtAuthenticationFilter 认证时已把 Users 设为 principal）
            Users user = getCurrentUser();
            if (user != null) {
                sysLog.setUserId(user.getId());
                sysLog.setUsername(user.getNickname());
            }

            // 注解上声明的模块和动作
            sysLog.setModule(operationLog.module());
            sysLog.setAction(operationLog.action());

            // 目标对象（在 proceed 前已查好）
            sysLog.setTargetId(targetId);
            sysLog.setTargetName(targetName);

            // IP 和浏览器 UA
            HttpServletRequest request = getRequest();
            if (request != null) {
                sysLog.setIp(getClientIp(request));
                sysLog.setUserAgent(request.getHeader("User-Agent"));
            }

            operationLogMapper.insert(sysLog);
        } catch (Exception e) {
            // ④ 日志记录失败绝不能影响业务，只打 warn
            log.warn("记录操作日志失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 提取操作对象 ID（约定式）
     * 取方法参数里第一个带 @PathVariable 且类型为 Long 的参数，作为操作对象 ID。
     * 管理端写操作几乎都是 @PathVariable Long id 开头，这个约定天然契合。
     * 没有匹配参数时返回 null（不影响日志记录）。
     */
    private Long extractTargetId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = signature.getMethod().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getAnnotation(PathVariable.class) != null) {
                // 兼容 Long 和 Integer 两种 ID 类型（如标签 id 是 Integer）
                if (args[i] instanceof Long) return (Long) args[i];
                if (args[i] instanceof Integer) return ((Integer) args[i]).longValue();
            }
        }
        return null;
    }

    /**
     * 根据模块和操作对象 ID 解析对象名称
     * 不同模块的对象存在不同表，这里按 module 分发查询。
     * 查询失败返回 null（不影响日志记录）。
     */
    private String resolveTargetName(String module, Long targetId) {
        if (targetId == null) return null;
        try {
            return switch (module) {
                case "用户管理", "通知管理" -> {
                    String nickname = userMapper.getNicknameById(targetId);
                    yield nickname;
                }
                case "博客管理" -> {
                    // 博客可能在线（blogs）或回收站（blogsBin），都查一下
                    Blogs blog = blogMapper.getBlogById(targetId);
                    if (blog != null) yield blog.getTitle();
                    Blogs binBlog = blogMapper.getBlogFromBinById(targetId);
                    yield binBlog != null ? binBlog.getTitle() : null;
                }
                case "标签管理" -> {
                    Tag tag = tagMapper.getTagById(targetId.intValue());
                    yield tag != null ? tag.getTagName() : null;
                }
                case "评论管理" -> {
                    Comments comment = commentMapper.getCommentById(targetId);
                    yield comment != null ? abbreviate(comment.getContent(), 20) : null;
                }
                case "角色管理" -> {
                    SysRole role = roleMapper.findById(targetId);
                    yield role != null ? role.getName() : null;
                }
                case "公告管理" -> {
                    SysAnnouncement announcement = announcementMapper.findById(targetId);
                    yield announcement != null ? announcement.getTitle() : null;
                }
                case "荣誉管理" -> {
                    Achievement achievement = achievementMapper.findById(targetId.intValue());
                    yield achievement != null ? achievement.getName() : null;
                }
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    /** 截断长文本（评论内容可能很长，只保留前 maxLen 字符） */
    private String abbreviate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /** 从 SecurityContext 取当前登录用户 */
    private Users getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Java 17 的 pattern matching instanceof：判断类型的同时直接拿到 user 变量
        if (auth != null && auth.getPrincipal() instanceof Users user) {
            return user;
        }
        return null;
    }

    /** 从 RequestContextHolder 取当前请求（切面里拿 HttpServletRequest 的标准方式） */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /**
     * 获取客户端真实 IP
     * 生产环境经过 Nginx 反向代理，remoteAddr 是 127.0.0.1，真实 IP 在 X-Forwarded-For 头里
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For 格式："客户端IP, 代理1IP, 代理2IP"，取第一个（最原始的客户端）
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
