package com.family.chat.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import com.family.chat.ChatApplication;
import com.family.chat.config.FilePathConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ChatImageUtil {

    private static final String TAG = "ChatImageUtil";

    /**
     * 在手机相册核心目录 DCIM/Camera/ 下创建图片副本
     */
    public static String makeLocalCopy(String originalPath) {
        try {
            File srcFile = new File(originalPath);
            if (!srcFile.exists()) return originalPath;

            // 🌟 1. 定位到系统相机/主相册目录：/storage/emulated/0/DCIM/Camera/
            File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            File cameraDir = new File(dcimDir, "Camera");

            if (!cameraDir.exists()) {
                boolean created = cameraDir.mkdirs(); // 如果 Camera 文件夹不存在则创建
                Log.d(TAG, "创建 Camera 目录结果: " + created);
            }

            // 2. 生成文件名 (例: YM_1700000000000.jpg)
            File destFile = new File(cameraDir, "YM_" + System.currentTimeMillis() + ".jpg");

            // 3. 复制字节流到 DCIM/Camera
            try (FileInputStream in = new FileInputStream(srcFile);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024 * 4];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            String cameraPath = destFile.getAbsolutePath();
            Log.d(TAG, "🎉 图片副本成功保存到 DCIM/Camera: " + cameraPath);

            // 🌟 4. 广播通知系统相册刷新（图片会瞬间直接出现在手机相册的主相机胶卷里！）
            scanFileToSystemGallery(cameraPath);

            return cameraPath; // 返回 DCIM/Camera 目录下的物理路径
        } catch (Exception e) {
            Log.e(TAG, "创建 DCIM 图片副本异常", e);
            return originalPath; // 发生异常时降级使用原路径
        }
    }

    /**
     * 广播通知系统图库刷新
     */
    private static void scanFileToSystemGallery(String filePath) {
        try {
            Context context = ChatApplication.getContext();
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(new File(filePath));
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
            Log.w("ChatImageUtil", "通知系统图库刷新失败: " + e.getMessage());
        }
    }

    /**
     * 🌟 大厂规范（微信同款）：保存头像到 App 专属沙盒目录
     * 路径: /storage/emulated/0/Android/data/com.family.chat/files/Pictures/avatars/my_avatar.jpg
     */
    public static String saveAvatarToAppDir(String sourcePath) {
        try {
            Context context = ChatApplication.getContext();
            File srcFile = new File(sourcePath);
            if (!srcFile.exists()) return sourcePath;

            // 1. 🌟 定位到 Android 规范的 App 外部沙盒目录：/Android/data/com.family.chat/files/Pictures/avatars/
            File externalPicturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File avatarDir = new File(externalPicturesDir, "avatars");

            if (!avatarDir.exists()) {
                boolean created = avatarDir.mkdirs(); // 自动创建 avatars 文件夹
                Log.d(TAG, "创建应用沙盒头像目录结果: " + created);
            }

            // 2. 规范固定命名 (当前用户的专属头像)
            File destFile = new File(avatarDir, "my_avatar.jpg");

            // 3. 复制字节流
            try (FileInputStream in = new FileInputStream(srcFile);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024 * 4];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            String appSandboxPath = destFile.getAbsolutePath();
            Log.d(TAG, "🎉 新头像已安全存入大厂规范沙盒目录: " + appSandboxPath);

            return appSandboxPath; // 返回受保护的沙盒路径
        } catch (Exception e) {
            Log.e(TAG, "保存沙盒头像异常", e);
            return sourcePath;
        }
    }

    /**
     * 🌟 保存/覆盖当前用户的固定头像文件 (例: avatar_user_10086.jpg)
     */
    public static String saveUserAvatarFixedPath(Long userId, String sourcePath) {
        try {
            File srcFile = new File(sourcePath);
            if (!srcFile.exists()) return sourcePath;

            // 1. 获取基于 userId 计算出的固定文件对象！
            File destFile = FilePathConfig.getFixedAvatarFile(userId);

            // 2. 复制字节流覆盖保存
            try (FileInputStream in = new FileInputStream(srcFile);
                 FileOutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024 * 4];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            String fixedPath = destFile.getAbsolutePath();
            Log.d("ChatImageUtil", "🎉 头像已按固定规则写入磁盘: " + fixedPath);
            return fixedPath;
        } catch (Exception e) {
            Log.e("ChatImageUtil", "头像固定路径落盘失败", e);
            return sourcePath;
        }
    }
}