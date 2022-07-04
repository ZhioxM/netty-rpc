package com.moon.netty.rpc.service.impl;

import com.moon.netty.rpc.annotation.server.RpcService;
import com.moon.netty.rpc.service.OrderService;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * @Author: Mzx
 * @Date: 2022/6/13 21:46
 */
@RpcService
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Override
    public String sumbit(String username) {
        log.debug("用户{}成功下单", username);
        return UUID.randomUUID().toString().substring(0, 10);
    }
}
