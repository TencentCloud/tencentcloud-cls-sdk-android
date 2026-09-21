package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

/**
 * Socket 相关辅助方法占位类。
 *
 * 历史版本中该类通过反射读取 {@link java.net.Socket} 内部 {@code impl.fd.descriptor} 拿到原生 fd
 * （方法 {@code getSocketFileDescriptor(Socket)}），存在两类严重风险，已被移除：
 * <ul>
 *     <li>fdsan 风险：拿到的 raw fd 若交给 JNI 使用 {@code unique_fd}/{@code android_fdsan_close_with_tag}
 *         管理，很容易与 Java 侧 {@link java.net.Socket#close()} 的 close 逻辑发生双关，触发
 *         {@code fdsan: attempted to close file descriptor ..., expected to be unowned, actually owned by unique_fd}
 *         的 SIGABRT。</li>
 *     <li>兼容性风险：{@code Socket.class.getDeclaredField("impl")} 属于 hidden API，在 Android 14+
 *         （TargetSdk 34+）灰名单收紧时可能返回 -1 或抛异常。</li>
 * </ul>
 *
 * 如后续确实需要拿 native fd，请通过 JNI 层直接创建/管理 socket，并使用 unique_fd 明确所有权，
 * 不要在 Java 侧反射暴露 fd。
 */
public class SocketHelper {

    private SocketHelper() {
    }
}
