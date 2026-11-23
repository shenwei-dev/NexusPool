package com.family.chat;

import android.app.Application;
import android.content.Context;

public class ChatApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    /**
     * 获取全局 Context 的静态方法
     */
    public static Context getContext() {
        return context;
    }
}
