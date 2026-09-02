package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.common.TokenStore;
import org.nep.nepsystem.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 公众认证接口（NEPS 公众端新增，纯新增端点，不影响现有管理员登录）：
 * POST /api/auth/login-public  - 公众登录（查 users 表，与 /api/auth/login 查 admins 表互不影响）
 * POST /api/auth/logout-public - 公众登出
 * 返回 token（UUID，与现有课程设计定位一致；正式认证体系接入后替换）
 */
@RestController
@RequestMapping("/api/auth")
public class PublicAuthController {

    @Autowired
    private UsersDao usersDao;

    @PostMapping("/login-public")
    public Result<Map<String, Object>> loginPublic(@RequestBody Map<String, String> body) {
        String username = body != null ? body.get("username") : null;
        String password = body != null ? body.get("password") : null;
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException(400, "用户名和密码不能为空");
        }
        Users user = usersDao.selectOne(new QueryWrapper<Users>()
                .eq("username", username)
                .last("limit 1"));
        if (user == null || !user.getPassword().equals(password)) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(403, "账号已被禁用");
        }
        String token = TokenStore.issue(TokenStore.Kind.USER, user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return Result.ok("登录成功", data);
    }

    @PostMapping("/logout-public")
    public Result<Void> logoutPublic(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            TokenStore.remove(authorization.substring("Bearer ".length()).trim());
        }
        return Result.ok("已退出登录", null);
    }
}