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
 * NetworkSocketBinder 实现类
 * 负责将 native 传入的 socket fd 绑定到指定的网络接口。
 *
 * <p><b>关于 fdsan 的说明：</b>
 * 本类曾经因 {@link ParcelFileDescriptor#fromFd(int)} 在某些 Android 版本 / OEM ROM 上会给
 * 传入的 fd 打上 unique_fd owner tag，导致 native 侧后续用裸 {@code close(fd)} 释放时触发
 * SIGABRT ({@code fdsan: attempted to close file descriptor X, expected to be unowned}) 而崩溃。
 *
 * <p>该问题已在 <b>native 层</b>（{@code libclsnetworkdiagnosis.so}）解决：
 * <ul>
 *     <li>{@code JNI_OnLoad} 中调用 {@code android_fdsan_set_error_level(WARN_ALWAYS)}
 *         把 fdsan 从 abort 降级为 warn；</li>
 *     <li>所有 native close 都改为 {@code cls_safe_close_fd()}，会先通过
 *         {@code android_fdsan_get_owner_tag} 拿到真实 tag 后 close。</li>
 * </ul>
 *
 * 因此本类可以恢复原始朴素写法，无需再做反射构造 FileDescriptor 或复杂的 detachFd 兜底。
 *
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
            //    ParcelFileDescriptor.fromFd 会 dup 一个 fd 并交由 unique_fd 管理，
            //    随后 pfd.close() 会关闭这个 dup fd（原始 socketFd 不变，由 native 层管理）。
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
            if (null == boundInetAddress) {
                CLSLog.w(TAG, "Failed to get network IP address, binding to network only");
                return socketFd;
            }

            // 3) 对原始 socketFd 做本地 IP/端口绑定
            FileDescriptor fd = pfd.getFileDescriptor();
            if ("icmp".equalsIgnoreCase(protocol)) {
                try {
                    Os.bind(fd, boundInetAddress, 0);
                    CLSLog.d(TAG, "ICMP socket bound to IP only: " + boundInetAddress.getHostAddress());
                } catch (Throwable e) {
                    CLSLog.w(TAG, "Failed to bind ICMP socket to IP: " + e.getMessage());
                }
            } else if ("udp".equalsIgnoreCase(protocol) || "tcp".equalsIgnoreCase(protocol)) {
                int bindPort = getUnusedHighPort();
                try {
                    Os.bind(fd, boundInetAddress, bindPort);
                    CLSLog.d(TAG, protocol.toUpperCase() + " socket bound to IP and port: "
                            + boundInetAddress.getHostAddress() + ":" + bindPort);
                } catch (Throwable e) {
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
            // 无论正常/异常，都关闭 pfd 释放 dup fd。
            // native 侧的 fd 是 socketFd（未通过 pfd 管理），这里 pfd.close 不会影响它。
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (IOException ignore) {
                }
            }
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