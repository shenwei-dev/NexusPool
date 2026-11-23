package com.family.chat.config;

import com.family.chat.BuildConfig;

/**
 * 服务端地址由 local.properties 注入的 BuildConfig 字段生成。
 * 请勿在源码中写入真实内网地址或生产域名。
 */
public class AppConfig {

    public static final String HTTP_BASE_URL =
            "http://" + BuildConfig.SERVER_HOST + ":" + BuildConfig.HTTP_PORT;
    public static final String WS_BASE_URL =
            "wss://" + BuildConfig.SERVER_HOST + ":" + BuildConfig.WS_PORT + "/ws";
}
