package com.moon.netty.rpc.config;


import com.moon.netty.rpc.protocol.serializer.Serializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 读取配置文件的配置类
 */
public abstract class Config {
    static Properties properties;

    static {
        try (InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static int getServerPort() {
        String value = properties.getProperty("server.port");
        if (value == null) {
            return 8080;
        } else {
            return Integer.parseInt(value);
        }
    }

    public static Serializer.Algorithm getSerializerAlgorithm() {
        // 从配置文件中选择序列化方法
        String value = properties.getProperty("serializer.algorithm");
        if (value == null) { // 默认使用JDK的序列化方式
            return Serializer.Algorithm.Java;
        } else {
            // 返回序列化器
            return Serializer.Algorithm.valueOf(value);
        }
    }
}