package org.nep.nepsystem.common;

import org.nep.nepsystem.exception.BizException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：把异常统一转换为 {code, message, data} 格式返回
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数类型不匹配（如 int 收到非数字字符串） */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        log.warn("参数类型错误: 参数 {} 需要类型 {}, 实际值: {}", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "?", e.getValue());
        return Result.fail(400, "参数格式错误: " + e.getName() + " 需要 " + (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "数字") + " 类型");
    }

    /** 参数校验异常（@Valid 触发） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        return Result.fail(400, msg);
    }

    /** 上传超限（BUG-002 修复）：返回 400 而非 500 */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUpload(org.springframework.web.multipart.MaxUploadSizeExceededException e) {
        return Result.fail(400, "文件大小超过限制（图片最大 5MB）");
    }

    /** 兜底异常（BUG-004 修复：脱敏，不向客户端泄露堆栈/内部信息；完整异常仍写日志） */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }
}