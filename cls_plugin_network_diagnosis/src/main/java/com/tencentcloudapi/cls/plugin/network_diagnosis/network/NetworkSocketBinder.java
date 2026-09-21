package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        if (socketFd < 0) {
            return -1;
        }
        ParcelFileDescriptor pfd = null;
        try {
            // 1) 将 socket 绑定到指定 Network。
            //    ParcelFileDescriptor.fromFd(int) 会 dup 出一个新的 fd 并用 unique_fd 打上 owner tag，
            //    如果我们随后手动 close(pfd)，一旦 framework 内部/GC 也去 close 该 dup fd 就会造成
            //    fdsan double-close crash（表现为 "attempted to close file descriptor X, expected to be
            //    unowned, actually owned by unique_fd 0x...")。
            //    因此这里在 network.bindSocket 结束后，调用 detachFd() 放弃 pfd 对 dup fd 的所有权，
            //    再通过 Os.close 无 tag 地关闭它，避免与 unique_fd tag 冲突。
            pfd = ParcelFileDescriptor.fromFd(socketFd);
            network.bindSocket(pfd.getFileDescriptor());

            // 2) 计算需要绑定的本地 IP
            InetAddress boundInetAddress;
            if (isIpv6) {
                boundInetAddress = getIPv6Address(network, interfaceName);
            } else {
                boundInetAddress = getIPv4Address(network, interfaceName);
                if (null == boundInetAddress) {
                    boundInetAddress = getIPv6Address(network, interfaceName);
                }
            }

            // 3) 释放 pfd 对 dup fd 的所有权（detachFd 返回 dup fd，且清除 unique_fd owner tag），
            //    然后我们自己关闭它。这样后续无论谁再持有该编号都不会触发 fdsan 冲突。
            int dupFd = pfd.detachFd();
            pfd = null; // 已 detach，不需要再 close
            try {
                Os.close(fromRawFd(dupFd));
            } catch (Throwable ignore) {
                // 关闭失败不影响主流程
            }

            if (null == boundInetAddress) {
                CLSLog.w(TAG, "Failed to get network IP address, binding to network only");
                return socketFd;
            }

            // 4) 对原始 socketFd 做本地 IP/端口绑定。
            //    直接使用一个非拥有型的 FileDescriptor 包装原始 fd 传给 Os.bind，绝不去 close 它 —
            //    该 fd 的所有权始终归 native 探测层（JNI unique_fd）所有。
            FileDescriptor rawFd = fromRawFd(socketFd);
            if ("icmp".equalsIgnoreCase(protocol)) {
                try {
                    Os.bind(rawFd, boundInetAddress, 0);
                    CLSLog.d(TAG, "ICMP socket bound to IP only: " + boundInetAddress.getHostAddress());
                } catch (Exception e) {
                    CLSLog.w(TAG, "Failed to bind ICMP socket to IP: " + e.getMessage());
                }
            } else if ("udp".equalsIgnoreCase(protocol) || "tcp".equalsIgnoreCase(protocol)) {
                int bindPort = getUnusedHighPort();
                try {
                    Os.bind(rawFd, boundInetAddress, bindPort);
                    CLSLog.d(TAG, protocol.toUpperCase() + " socket bound to IP and port: "
                            + boundInetAddress.getHostAddress() + ":" + bindPort);
                } catch (Exception e) {
                    CLSLog.w(TAG, "Failed to bind " + protocol.toUpperCase()
                            + " socket to IP and port: " + e.getMessage());
                }
            }

            return socketFd;
        } catch (Throwable e) {
            CLSLog.e(TAG, "Failed to bind socket to network: " + e.getMessage());
            CLSLog.printStackTrace(e);
            return -1;
        } finally {
            // 如果 pfd 因为异常没有 detach，这里安全释放
            if (pfd != null) {
                try {
                    int leftFd = pfd.detachFd();
                    try {
                        Os.close(fromRawFd(leftFd));
                    } catch (Throwable ignore) {
                    }
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * 构造一个"非拥有型"的 {@link FileDescriptor}，仅用于把 raw fd 传递给 {@link Os#bind}
     * 等系统调用。返回的对象不会被 fdsan 打 tag，也不应该被 close —— fd 的真正所有者是 JNI
     * 探测层。使用反射设置 FileDescriptor 内部字段以适配各版本 Android。
     */
    private static FileDescriptor fromRawFd(int fd) {
        FileDescriptor result = new FileDescriptor();
        try {
            // 优先尝试 public API (API 27+)
            Method setInt$ = FileDescriptor.class.getMethod("setInt$", int.class);
            setInt$.invoke(result, fd);
            return result;
        } catch (Throwable ignore) {
        }
        try {
            Field descriptor = FileDescriptor.class.getDeclaredField("descriptor");
            descriptor.setAccessible(true);
            descriptor.setInt(result, fd);
        } catch (Throwable e) {
            CLSLog.e(TAG, "fromRawFd failed: " + e.getMessage());
        }
        return result;
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