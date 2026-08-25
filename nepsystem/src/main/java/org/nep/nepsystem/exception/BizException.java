package org.nep.nepsystem.exception;

/**
 * 自定义业务异常：业务校验失败时抛出，由 GlobalExceptionHandler 统一处理
 */
public class BizException extends RuntimeException {
    private Integer code = 500;

    public BizException(String message) {
        super(message);
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
