package com.family.chat.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.family.chat.ChatApplication;
import com.family.chat.R;
import com.family.chat.config.FilePathConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 头像统一管理工具。
 *
 * <p>职责一: userId -> avatarUrl 的本地缓存 (内存 Map + SharedPreferences 持久化),
 * 各数据源(好友列表 / 会话列表 / 好友详情 / WS 消息帧)在拿到头像时调用 {@link #put} 填充,
 * 聊天/通话等只拿得到 userId 的场景通过 {@link #get} 读取。
 *
 * <p>职责二: 统一的 Glide 加载入口, 内置 AI 助手特判与占位图。
 *
 * <p>职责三: 好友头像本地固定路径文件缓存。后端下发的是临时签名 URL, 每次请求签名参数都不同,
 * Glide 以完整 URL 作缓存键必然每次穿透网络, 导致占位图闪烁; 故首次下载后落盘到
 * {@link FilePathConfig#getFixedAvatarFile} 固定路径, 后续绑定直接读本地文件, 后台静默比对更新。
 */
public final class AvatarManager {

    private static final String TAG = "AvatarManager";

    /** AI 助手固定用户ID */
    public static final long AI_USER_ID = 99999L;

    private static final String PREF_NAME = "avatar_cache";
    private static final String KEY_PREFIX = "avatar_";

    /** 一级内存缓存, 进程内零 IO 读取 */
    private static final Map<Long, String> MEMORY_CACHE = new ConcurrentHashMap<>();

    /** 正在后台下载头像的用户集合, 防止列表快速刷新时重复提交下载任务 */
    private static final Set<Long> DOWNLOADING_USERS = ConcurrentHashMap.newKeySet();

    private static volatile SharedPreferences preferences;

    private AvatarManager() {}

    private static SharedPreferences getPreferences() {
        if (preferences == null) {
            synchronized (AvatarManager.class) {
                if (preferences == null) {
                    preferences = ChatApplication.getContext()
                            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                }
            }
        }
        return preferences;
    }

    /**
     * 写入用户头像缓存 (空值/非法 URL 不写入, 不覆盖已有值)
     */
    public static void put(Long userId, String avatarUrl) {
        if (userId == null || !isValidAvatarUrl(avatarUrl)) {
            return;
        }
        MEMORY_CACHE.put(userId, avatarUrl);
        getPreferences().edit().putString(KEY_PREFIX + userId, avatarUrl).apply();
    }

    /**
     * 校验头像 URL 有效性: 仅接受 http(s) 完整地址,
     * 拦截注册时写入的 "/default_avatar.jpg" 等哨兵值与相对路径
     */
    private static boolean isValidAvatarUrl(String avatarUrl) {
        return !TextUtils.isEmpty(avatarUrl)
                && (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"));
    }

    /**
     * 读取用户头像缓存, 未命中返回 null
     */
    public static String get(Long userId) {
        if (userId == null) {
            return null;
        }
        String url = MEMORY_CACHE.get(userId);
        if (url != null) {
            return url;
        }
        url = getPreferences().getString(KEY_PREFIX + userId, null);
        if (url != null) {
            MEMORY_CACHE.put(userId, url);
        }
        return url;
    }

    /**
     * 按 userId 加载对方头像: AI 助手特判 -> 本人头像 -> 好友本地文件缓存优先。
     */
    public static void load(ImageView imageView, Long userId) {
        if (imageView == null || userId == null) {
            return;
        }
        if (userId == AI_USER_ID) {
            imageView.setImageResource(R.drawable.ic_chat_ai_avatar);
            return;
        }
        if (userId == SessionManager.getUserId()) {
            loadMyAvatar(imageView);
            return;
        }
        loadFriendAvatar(imageView, userId);
    }

    /**
     * 加载好友头像: 本地固定路径文件命中时直接渲染(无占位图闪烁),
     * 同时后台静默比对远端头像是否变更; 未命中才走网络并落盘缓存。
     */
    private static void loadFriendAvatar(ImageView imageView, Long userId) {
        String url = get(userId);
        File localFile = FilePathConfig.getFixedAvatarFile(userId);

        if (localFile.exists() && localFile.length() > 0) {
            // 附加文件修改时间签名: 固定路径文件被新头像覆盖后, 强制 Glide 缓存失效
            Glide.with(imageView.getContext())
                    .load(localFile)
                    .signature(new ObjectKey(localFile.lastModified()))
                    // 复用当前图像作占位, 复用 ViewHolder 时避免默认头像一闪而过
                    .placeholder(imageView.getDrawable())
                    .error(android.R.drawable.sym_def_app_icon)
                    .into(imageView);
            if (isValidAvatarUrl(url)) {
                downloadAndCacheAvatar(userId, url);
            }
            return;
        }

        if (isValidAvatarUrl(url)) {
            // 首次加载无本地缓存: 网络渲染的同时后台落盘, 下次绑定即可秒开
            Glide.with(imageView.getContext())
                    .load(url)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .into(imageView);
            downloadAndCacheAvatar(userId, url);
            return;
        }

        imageView.setImageResource(android.R.drawable.sym_def_app_icon);
    }

    /**
     * 后台下载头像并写入固定路径。内容未变化时不覆盖文件,
     * 防止 lastModified 抖动导致 Glide 缓存无谓失效。
     */
    private static void downloadAndCacheAvatar(Long userId, String url) {
        if (!DOWNLOADING_USERS.add(userId)) {
            return;
        }
        AppExecutors.io().execute(() -> {
            try {
                File downloaded = Glide.with(ChatApplication.getContext())
                        .asFile()
                        .load(url)
                        .submit()
                        .get();
                if (downloaded == null || !downloaded.exists()) {
                    return;
                }
                File fixedFile = FilePathConfig.getFixedAvatarFile(userId);
                if (fixedFile.exists() && isSameContent(downloaded, fixedFile)) {
                    return;
                }
                // 先写临时文件再原子替换, 避免并发读取到写了一半的头像
                File tempFile = new File(fixedFile.getParentFile(), fixedFile.getName() + ".tmp");
                copyFile(downloaded, tempFile);
                if (!tempFile.renameTo(fixedFile)) {
                    copyFile(downloaded, fixedFile);
                }
                Log.d(TAG, "Friend avatar cached to fixed path. [userId=" + userId + "]");
            } catch (Exception e) {
                Log.w(TAG, "Failed to cache friend avatar: " + e.getMessage());
            } finally {
                DOWNLOADING_USERS.remove(userId);
            }
        });
    }

    /**
     * 逐字节比对两个文件内容, 头像体积通常远小于 100KB, 开销可忽略
     */
    private static boolean isSameContent(File fileA, File fileB) {
        if (fileA.length() != fileB.length()) {
            return false;
        }
        try (InputStream inA = new FileInputStream(fileA);
             InputStream inB = new FileInputStream(fileB)) {
            int byteA;
            while ((byteA = inA.read()) != -1) {
                if (byteA != inB.read()) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void copyFile(File source, File target) throws Exception {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    /**
     * 加载本人头像: 优先本地文件副本, 其次 OSS URL, 均无则占位图。
     * 本地文件为固定路径 my_avatar.jpg 且会被覆盖更新,
     * 故跳过内存/磁盘缓存并附加文件修改时间签名, 防止 Glide 命中旧图。
     */
    public static void loadMyAvatar(ImageView imageView) {
        if (imageView == null) {
            return;
        }
        String localPath = SessionManager.getLocalAvatarPath();
        if (!TextUtils.isEmpty(localPath)) {
            File localFile = new File(localPath);
            if (localFile.exists() && localFile.length() > 0) {
                Glide.with(imageView.getContext())
                        .load(localFile)
                        .signature(new ObjectKey(localFile.lastModified()))
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .error(android.R.drawable.sym_def_app_icon)
                        .into(imageView);
                return;
            }
        }
        loadUrl(imageView, SessionManager.getOssAvatarUrl());
    }

    /**
     * 按 URL 加载头像, 空 URL/非法 URL/加载失败均回退占位图
     */
    public static void loadUrl(ImageView imageView, String avatarUrl) {
        if (imageView == null) {
            return;
        }
        if (!isValidAvatarUrl(avatarUrl)) {
            imageView.setImageResource(android.R.drawable.sym_def_app_icon);
            return;
        }
        Glide.with(imageView.getContext())
                .load(avatarUrl)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .into(imageView);
    }
}
