package com.moon.netty.rpc.schedule;
import com.moon.netty.rpc.exception.TimeoutException;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import static com.moon.netty.rpc.transport.client.RpcRequestFactory.*;

/**
 * @author mzx
 */
@Slf4j
public class TimeoutCheckJob implements Runnable{

    @Override
    public void run() {
        log.info("开始检测RPC超时...");
        Iterator<Map.Entry<Integer, Date>> it = UNPROCESSED_RPC_REQUEST_TIMEOUT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Date> dateEntry = it.next();
            Date expireTime = dateEntry.getValue();
            if(new Date().after(expireTime)) {
                Integer id = dateEntry.getKey();
                it.remove(); // 删除这个请求
                Promise<Object> promise = UNPROCESSED_RPC_REQUEST_PROMISES.remove(id);
                promise.setFailure(new TimeoutException("RPC 调用超时"));
            }
        }
    }
}
