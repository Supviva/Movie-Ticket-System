package com.movieticket.backend.db;

/**
 * 业务异常。用于向调用方传递「可读的业务错误」，
 * 例如「所选座位已被他人购买，请重新选择」。
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
