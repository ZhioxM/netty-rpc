package com.moon.netty.rpc.exception;


/**
 * 客户端调用的超时异常
 * @author mzx
 */
public class TimeoutException extends RuntimeException{

    public TimeoutException() {}

    public TimeoutException(String msg) {
        super(msg);
    }
}
