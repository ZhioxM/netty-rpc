package com.moon.netty.rpc.message;

@Deprecated
public class PongMessage extends Message {
    @Override
    public int getMessageType() {
        return PongMessage;
    }
}
