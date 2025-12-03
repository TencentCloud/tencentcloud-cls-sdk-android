package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

/**
 * Socket绑定接口
 * 用于将socket绑定到指定网卡
 * @author farmerx
 */
public interface SocketBinder {
    /**
     * 将socket绑定到指定网卡
     * @param socketFd socket文件描述符
     * @param protocol 协议类型（"icmp", "udp", "tcp"）
     * @param isIpv6 是否IPv6
     * @return 绑定后的socket fd，失败返回-1
     * 注意：networkId 由 SocketBinder 实现类内部管理，不需要传入
     */
    int bindSocketToNetwork(int socketFd, String protocol, boolean isIpv6);
}
