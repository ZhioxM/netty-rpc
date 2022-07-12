package com.moon.netty.rpc.message;

import lombok.Getter;
import lombok.ToString;

/**
 * RPC 请求消息
 */
@Getter
@ToString(callSuper = true)
public class RpcRequestMessage extends Message {

    /**
     * RPC 调用超时
     * 单位默认是ms
     */
    private long timeout;
    /**
     * 调用的接口全限定名，服务端根据它找到实
     */


    private String interfaceName;
    /**
     * 调用接口中的方法名
     */
    private String methodName;
    /**
     * 方法返回类型
     */
    private Class<?> returnType;
    /**
     * 方法参数类型数组
     */
    private Class[] parameterTypes;

    /**
     * 方法参数值数组
     */
    private Object[] parameterValue;

    public RpcRequestMessage(int sequenceId, long timeout,  String interfaceName, String methodName, Class<?> returnType, Class[] parameterTypes, Object[] parameterValue) {
        super.setSequenceId(sequenceId);
        this.timeout = timeout;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        this.parameterValue = parameterValue;
    }

    @Override
    public int getMessageType() {
        return RPC_MESSAGE_TYPE_REQUEST;
    }
}
