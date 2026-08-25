package org.nep.nepsystem.ctrl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.nep.nepsystem.bean.Users;
import org.nep.nepsystem.common.PageResult;
import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.dao.UsersDao;
import org.nep.nepsystem.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理接口
 */
@RestController
@RequestMapping("/api/users")
public class UsersController {

    @Autowired
    private UsersDao usersDao;

    /** 分页查询 */
    @GetMapping("/page")
    public Result<PageResult<Users>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String keyword) {
        QueryWrapper<Users> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like("username", keyword).or().like("nickname", keyword));
        }
        qw.orderByDesc("create_time");
        IPage<Users> p = usersDao.selectPage(new Page<>(page, size), qw);
        return Result.ok(PageResult.of(p));
    }

    /** 新增 */
    @PostMapping
    public Result<Void> add(@RequestBody Users user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            throw new BizException(400, "用户名和密码不能为空");
        }
        Integer cnt = usersDao.selectCount(new QueryWrapper<Users>().eq("username", user.getUsername()));
        if (cnt > 0) {
            throw new BizException("用户名已存在: " + user.getUsername());
        }
        if (user.getRole() == null) user.setRole("USER");
        if (user.getStatus() == null) user.setStatus(1);
        usersDao.insert(user);
        return Result.ok("新增成功", null);
    }

    /** 修改 */
    @PutMapping
    public Result<Void> update(@RequestBody Users user) {
        if (user.getId() == null) {
            throw new BizException(400, "缺少用户ID");
        }
        usersDao.updateById(user);
        return Result.ok("修改成功", null);
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        usersDao.deleteById(id);
        return Result.ok("删除成功", null);
    }

    /** 启用/禁用 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Integer id, @RequestParam Integer status) {
        Users u = new Users();
        u.setId(id);
        u.setStatus(status);
        usersDao.updateById(u);
        return Result.ok("状态已更新", null);
    }
}