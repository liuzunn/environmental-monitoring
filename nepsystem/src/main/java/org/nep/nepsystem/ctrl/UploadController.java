package org.nep.nepsystem.ctrl;

import org.nep.nepsystem.common.Result;
import org.nep.nepsystem.exception.BizException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传接口（BUG-002 修复，新增）：
 * POST /api/upload （multipart/form-data，字段名 file）
 * - 仅允许图片：jpg/jpeg/png/webp/gif；大小上限 5MB
 * - 存储目录：工作目录 ./uploads/yyyyMMdd/uuid.ext
 * - 返回：{url:"/uploads/yyyyMMdd/xxx.ext"}（经 /uploads/** 静态映射访问）
 * 兼容：不改变既有接口协议；前端将 URL 写入 attachments.filePath / record.images
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_SIZE = 5 * 1024 * 1024;
    private static final String[] ALLOWED = {"jpg", "jpeg", "png", "webp", "gif"};

    @PostMapping
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(400, "文件大小不能超过 5MB");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        boolean ok = false;
        for (String a : ALLOWED) {
            if (a.equals(ext)) { ok = true; break; }
        }
        if (!ok) {
            throw new BizException(400, "仅支持图片格式: jpg/jpeg/png/webp/gif");
        }
        try {
            String dirName = new SimpleDateFormat("yyyyMMdd").format(new Date());
            File dir = new File("uploads", dirName);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new BizException(500, "存储目录创建失败");
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            File target = new File(dir, fileName);
            file.transferTo(target.getAbsoluteFile());
            Map<String, Object> data = new HashMap<>();
            data.put("url", "/uploads/" + dirName + "/" + fileName);
            data.put("size", file.getSize());
            data.put("name", original);
            return Result.ok("上传成功", data);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "文件保存失败: " + e.getMessage());
        }
    }
}
