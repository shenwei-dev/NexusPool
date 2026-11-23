package com.family.chat.config;

import android.content.Context;
import android.os.Environment;
import com.family.chat.ChatApplication;
import java.io.File;

/**
 * 全局路径配置类：统一管理 App 所有文件的存储目录与固定命名规则
 */
public class FilePathConfig {

    /**
     * 获取大厂规范的外部沙盒根目录 (/Android/data/com.family.chat/files/)
     */
    public static File getAppExternalFilesDir() {
        Context context = ChatApplication.getContext();
        File filesDir = context.getExternalFilesDir(null);
        if (filesDir == null) {
            filesDir = context.getFilesDir(); // 备用内部存储
        }
        return filesDir;
    }

    /**
     * 获取头像总目录 (/Android/data/com.family.chat/files/Pictures/avatars/)
     */
    public static File getAvatarDir() {
        File picturesDir = new File(getAppExternalFilesDir(), Environment.DIRECTORY_PICTURES);
        File avatarDir = new File(picturesDir, "avatars");
        if (!avatarDir.exists()) {
            avatarDir.mkdirs();
        }
        return avatarDir;
    }

    /**
     * 🌟 核心规则公式：根据用户 ID，推出【全局固定唯一】的本地头像文件！
     * 例如: /Android/data/com.family.chat/files/Pictures/avatars/avatar_user_10086.jpg
     */
    public static File getFixedAvatarFile(Object userId) {
        if (userId == null) {
            return new File(getAvatarDir(), "avatar_user_default.jpg");
        }
        return new File(getAvatarDir(), "avatar_user_" + userId + ".jpg");
    }
}