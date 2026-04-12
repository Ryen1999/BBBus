package com.bbbus.contentservice.enums;

public enum ErrorStatusEnum {
    NOT_FOUND(404, "未找到该资源"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    NOT_ACCEPTABLE(406, "请求的资源不可接受"),
    REQUEST_TIMEOUT(408, "请求超时"),
    CONFLICT(409, "请求冲突"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    UNAUTHORIZED(501, "未授权"),

    SERVICE_UNAVAILABLE(503, "服务不可用");
    //未授权


    private int code;
    private String message;

    ErrorStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }


    public static ErrorStatusEnum getByCode(int code) {
        for (ErrorStatusEnum value : ErrorStatusEnum.values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
