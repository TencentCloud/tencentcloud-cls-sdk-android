package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.os.ParcelFileDescriptor;
import android.system.Os;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import com.tencentcloudapi.cls.android.CLSLog;

/**
 * NetworkSocketBinder实现类
 * 负责将socket绑定到指定的网络接口
 * @author farmerx
 */
@SuppressLint({"NewApi"})
public class NetworkSocketBinder implements SocketBinder {
    private static final String TAG = NetworkSocketBinder.class.getCanonicalName();

    private final Network network;
    private final String interfaceName;

    public NetworkSocketBinder(Network network, String interfaceName) {
        this.network = network;
        this.interfaceName = interfaceName;
    }

    @Override
    public int bindSocketToNetwork(int socketFd, String protocol, boolean isIpv6) {
        if (null == network) {
            return -1;
        }
        ParcelFileDescriptor pfd = null;
        try {
            pfd = ParcelFileDescriptor.fromFd(socketFd);
            network.bindSocket(pfd.getFileDescriptor());
            // 根据协议类型进行不同的IP/端口绑定
            FileDescriptor fd = pfd.getFileDescriptor();
            InetAddress boundInetAddress = null;

            // 获取网卡对应的IP地址
            if (isIpv6) {
                boundInetAddress = getIPv6Address(network, interfaceName);
            } else {
                // 对于IPv4，优先获取IPv4地址
                boundInetAddress = getIPv4Address(network, interfaceName);
                if (null == boundInetAddress) {
                    boundInetAddress = getIPv6Address(network, interfaceName);
                }
            }
            if (null == boundInetAddress) {
                CLSLog.w(TAG, "Failed to get network IP address, binding to network only");
                // fromFd不会关闭dup的socket，可以安全关闭
                pfd.close();
                return socketFd;
            }

            // 根据协议类型进行不同的绑定
            if ("icmp".equalsIgnoreCase(protocol)) {
                // ICMP协议：只绑定IP地址，不绑定端口
                try {
                    Os.bind(fd, boundInetAddress, 0);  // 端口为0表示不绑定特定端口
                    CLSLog.d(TAG, "ICMP socket bound to IP only: " + boundInetAddress.getHostAddress());
                } catch (Exception e) {
                    CLSLog.w(TAG, "Failed to bind ICMP socket to IP: " + e.getMessage());
                    // 如果IP绑定失败，至少已经绑定了Network
                }
            } else if ("udp".equalsIgnoreCase(protocol) || "tcp".equalsIgnoreCase(protocol)) {
                // UDP/TCP协议：绑定IP地址和端口
                int bindPort = getUnusedHighPort();
                try {
                    Os.bind(fd, boundInetAddress, bindPort);
                    CLSLog.d(TAG, protocol.toUpperCase() + " socket bound to IP and port: " +
                            boundInetAddress.getHostAddress() + ":" + bindPort);
                } catch (Exception e) {
                    CLSLog.w(TAG, "Failed to bind " + protocol.toUpperCase() + " socket to IP and port: " + e.getMessage());
                }
            }

            // 关闭ParcelFileDescriptor
            // fromFd不会关闭dup的socket，可以安全关闭
            // 绑定状态在socket资源级别共享，原始socket会继承绑定状态
            pfd.close();

            // 返回dup的socket fd（仍然有效）
            // JNI层使用原始socket进行探测，原始socket已继承绑定状态
            return socketFd;
        } catch (IOException e) {
            CLSLog.e(TAG, "Failed to bind socket to network: " + e.getMessage());
            CLSLog.printStackTrace(e);
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (IOException ignored) {}
            }
            return -1;
        } catch (Exception e) {
            CLSLog.e(TAG, "Failed to bind socket: " + e.getMessage());
            CLSLog.printStackTrace(e);
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (IOException ignored) {}
            }
            return -1;
        }
    }

    /**
     * 获取网络对应的IPv6地址
     */
    private static InetAddress getIPv6Address(Network network, String interfaceName) {
        try {
            // 方法1：通过LinkProperties查找IPv6地址
            ConnectivityManager cm = (ConnectivityManager) Utils.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
            LinkProperties linkProperties = cm.getLinkProperties(network);
            if (linkProperties != null) {
                for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                    InetAddress address = linkAddress.getAddress();
                    if (!address.isLoopbackAddress() && address.getAddress().length == 16) {
                        return address;
                    }
                }
            }

            // 方法2：通过接口名称查找IPv6地址
            if (interfaceName != null && !interfaceName.isEmpty()) {
                NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
                if (networkInterface != null) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress() && address.getAddress().length == 16) {
                            return address;
                        }
                    }
                }
            }
        } catch (Exception e) {
            CLSLog.e(TAG, "Failed to get IPv6 address: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取网络对应的IPv4地址
     */
    private static InetAddress getIPv4Address(Network network, String interfaceName) {
        try {
            // 方法1：通过LinkProperties查找IPv4地址
            ConnectivityManager cm = (ConnectivityManager) Utils.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
            LinkProperties linkProperties = cm.getLinkProperties(network);
            if (linkProperties != null) {
                for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                    InetAddress address = linkAddress.getAddress();
                    if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                        return address;
                    }
                }
            }

            // 方法2：通过接口名称查找IPv4地址
            if (interfaceName != null && !interfaceName.isEmpty()) {
                NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
                if (networkInterface != null) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                            return address;
                        }
                    }
                }
            }
        } catch (Exception e) {
            CLSLog.e(TAG, "Failed to get IPv4 address: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取一个未使用的高端口，用于绑定socket
     * 使用高端口范围（30000-65535）确保目标主机返回ICMP端口不可达消息
     */
    private static int getUnusedHighPort() {
        // 使用固定的高端口号，避免常用端口范围
        // 选择一个不太可能被占用的端口：35353
        int port = 35353;

        // 可以基于时间戳添加随机性，但保持相对固定以便调试
        long timeSeed = System.currentTimeMillis();
        int randomOffset = (int)(timeSeed % 6565);
        port = 33434 + randomOffset;

        // 确保端口在有效范围内
        if (port > 39999 || port < 33434) {
            port = 35353; // 回退到默认值
        }

        CLSLog.d(TAG, "Using high port for binding: " + port);
        return port;
    }
}