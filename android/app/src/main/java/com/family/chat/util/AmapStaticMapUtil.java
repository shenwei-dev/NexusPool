package com.family.chat.util;

import com.family.chat.BuildConfig;

public class AmapStaticMapUtil {

    // 高德 WebKey 由 local.properties 注入, 未配置时为空串
    private static final String AMAP_WEB_KEY = BuildConfig.AMAP_WEB_KEY;

    /**
     * 根据经纬度生成高德静态地图图片 URL
     */
    public static String getStaticMapUrl(double longitude, double latitude) {
        String locationString = longitude + "," + latitude;
        return "https://restapi.amap.com/v3/staticmap?"
                + "location=" + locationString
                + "&zoom=15"
                + "&size=400*200"
                + "&markers=mid,0x07C160,A:" + locationString
                + "&key=" + AMAP_WEB_KEY;
    }
}
