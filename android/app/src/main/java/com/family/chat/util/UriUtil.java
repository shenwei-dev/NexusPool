package com.family.chat.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Uri 路径工具类 (安全兼容 Android 8.0 ~ Android 14+)
 */
public class UriUtil {

    private static final String TAG = "UriUtil";

    /**
     * 将 android.net.Uri 安全转换为可读的本地物理文件路径
     */
    public static String getFilePathFromUri(Context context, Uri uri) {
        if (uri == null) return null;

        // 1. 如果 Scheme 是 "file"，直接返回物理路径
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        // 2. 如果 Scheme 是 "content"，安全复制字节流到 Cache 目录 (100% 兼容 Android 10-14 分区存储)
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try {
                File cacheDir = new File(context.getCacheDir(), "avatar_temp");
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }

                // 创建临时缓存文件
                File tempFile = new File(cacheDir, "TEMP_AVATAR_" + System.currentTimeMillis() + ".jpg");

                try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                     FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                    if (inputStream == null) return null;

                    byte[] buffer = new byte[1024 * 4];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                }

                return tempFile.getAbsolutePath(); // 返回安全的缓存物理路径
            } catch (Exception e) {
                Log.e(TAG, "从 Uri 读取图片流发生异常", e);
            }
        }

        return null;
    }
}