package com.moon.netty.rpc.loadBalancer.impl;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.moon.netty.rpc.loadBalancer.LoadBalancer;

import java.util.*;

public class IpHashLoadBalancer implements LoadBalancer {
    @Override
    public Instance getInstance(List<Instance> list) {
        return null;
    }
}


//一致性hash 利用treemap实现
class ConsistentHash {
    //TreeMap中的key表示服务器的hash值，value表示服务器。模拟一个哈希环
    private static final TreeMap<Integer, String> Nodes = new TreeMap<>();
    private static int VIRTUAL_NODES = 160; //虚拟节点个数，用户指定，默认160
    private static List<Instance> instances = new ArrayList<>(); //真实物理节点集合

    public ConsistentHash(List<Instance> instances, int VIRTUAL_NODES) {
        this.instances = instances;
        this.VIRTUAL_NODES = VIRTUAL_NODES;
    }

    public static HashMap<String, Instance> map = new HashMap<>();//将服务实例与ip地址一一映射

    //预处理 形成哈希环
    static {
        //程序初始化，将所有的服务器(真实节点与虚拟节点)放入Nodes（底层为红黑树）中
        for (Instance instance : instances) {
            String ip = instance.getIp();
            Nodes.put(getHash(ip), ip);
            map.put(ip, instance);
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                int hash = getHash(ip + "#" + i);
                Nodes.put(hash, ip);
                map.put(ip, instance);
            }
        }
    }

    //得到Ip地址
    public String getServer(String clientInfo) {
        int hash = getHash(clientInfo);
        //得到大于该Hash值的子红黑树
        SortedMap<Integer, String> subMap = Nodes.tailMap(hash);
        //获取该子树最小元素
        Integer nodeIndex = subMap.firstKey();
        //没有大于该元素的子树 取整树的第一个元素
        if (nodeIndex == null) {
            nodeIndex = Nodes.firstKey();
        }
        return Nodes.get(nodeIndex);
    }

    //使用FNV1_32_HASH算法计算服务器的Hash值,这里不使用重写hashCode的方法，最终效果没区别
    private static int getHash(String str) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++) {
            hash = (hash ^ str.charAt(i)) * p;
            hash += hash << 13;
            hash ^= hash >> 7;
            hash += hash << 3;
            hash ^= hash >> 17;
            hash += hash << 5;
            //如果算出来的值为负数 取其绝对值
            if (hash < 0) {
                hash = Math.abs(hash);
            }
        }
        return hash;
    }
}
