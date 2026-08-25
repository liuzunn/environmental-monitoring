package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.bean.Admins;
import org.nep.nepsystem.dao.AdminsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admins")
public class AdminsCtrl {
    @Autowired
    AdminsDao adminsDao;

    @RequestMapping("/insert")
    @ResponseBody
    public String insert(){
        Admins admins=new Admins();
        admins.setAdminId(2);
        admins.setAdminCode("tom");
        admins.setPassword("111");
        admins.setRemarks("xin");
        //调用dao
        adminsDao.insert(admins);
        System.out.println("insert success");
        return "success";
    }
    @RequestMapping("/update")
    @ResponseBody
    public String update(){
        Admins admins=new Admins();
        //注意  根据主键修改 传递2 必须在数据库表中主键值有2
        //修改主键为2的管理员 密码为123
        admins.setAdminId(2);
        admins.setPassword("123");
        adminsDao.updateById(admins);
        System.out.println("update success");
        return "success";
    }
    @RequestMapping("/delete")
    @ResponseBody
    public String delete(){
        //根据主键删除   2 数据库表中得有主键值2
        adminsDao.deleteById(2);
        System.out.println("delete success");
        return "success";
    }

}
