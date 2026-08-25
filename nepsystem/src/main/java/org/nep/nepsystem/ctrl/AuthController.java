package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.bean.Admins;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.AdminsDao;
import org.nep.nepsystem.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证接口：登录/登出（课程设计定位：返回简单 token 即可）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AdminsDao adminsDao;

    /** 登录：账号密码校验，成功返回 token */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String adminCode = body.get("adminCode");
        String password = body.get("password");
        if (adminCode == null || password == null) {
            throw new BizException(400, "账号和密码不能为空");
        }
        Admins admin = adminsDao.selectOne(new QueryWrapper<Admins>()
                .eq("admin_code", adminCode)
                .last("limit 1"));
        if (admin == null || !admin.getPassword().equals(password)) {
            throw new BizException(401, "账号或密码错误");
        }
        // 简单 token：UUID（课程设计够用，生产应换 JWT）
        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("adminId", admin.getAdminId());
        data.put("adminCode", admin.getAdminCode());
        return Result.ok("登录成功", data);
    }

    /** 登出 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok("已退出登录", null);
    }
}
