package org.nep.nepsystem.config;

import org.nep.nepsystem.common.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 认证拦截器（BUG-003 修复）：
 * - 拦截 /api/**（除登录登出入口）
 * - 请求携带 Authorization: Bearer <token> 时，必须为有效未过期令牌（否则 401）
 * - 未携带 Authorization 的请求放行（保持既有匿名只读/内部调用兼容；
 *   身份有效性仍由各 Service 依据 X-User-Id/X-Admin-Id + 归属关系校验）
 * - 登出端点带 token 时移除
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        // 登录/登出入口放行（登录签发；登出由 Controller 处理 token 移除）
        if (uri.endsWith("/auth/login") || uri.endsWith("/auth/login-public")
                || uri.endsWith("/auth/logout") || uri.endsWith("/auth/logout-public")) {
            return true;
        }
        // OPTIONS 预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return true; // 未携带 token：保持兼容（Service 层按身份头/归属校验）
        }
        String token = auth.substring("Bearer ".length()).trim();
        if (TokenStore.validate(token) == null) {
            // 与项目错误约定一致：HTTP 200 + code=401（前端拦截器按 code 统一处理）
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
            return false;
        }
        return true;
    }
}