package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import static com.google.common.base.Enums.getField;

import android.annotation.SuppressLint;

import com.tencentcloudapi.cls.android.CLSLog;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.Socket;

public class SocketHelper {
    public static int getSocketFileDescriptor(Socket socket) {
        try {
// 通过反射获取 FileDescriptor 和 int fd
            Field implField = Socket.class.getDeclaredField("impl");
            implField.setAccessible(true);
            Object impl = implField.get(socket);

            Field fdField = impl.getClass().getDeclaredField("fd");
            fdField.setAccessible(true);
            FileDescriptor fd = (FileDescriptor) fdField.get(impl);

            Field fdIntField = FileDescriptor.class.getDeclaredField("descriptor");
            fdIntField.setAccessible(true);
            int fdInt = fdIntField.getInt(fd);
            return fdInt;
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
            return -1;
        }
    }

    private static int getFileDescriptorValue(FileDescriptor fd) {
        try {
            Field fdField = FileDescriptor.class.getDeclaredField("descriptor");
            fdField.setAccessible(true);
            return fdField.getInt(fd);
        } catch (Exception e) {
            CLSLog.printStackTrace(e);
            return -1;
        }
    }

}
