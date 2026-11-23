package com.family.chat.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.Arrays;
import java.util.List;

public class HomeViewModel extends ViewModel {

    public MutableLiveData<List<String>> listData = new MutableLiveData<>();

    public void loadMessages() {
        listData.setValue(Arrays.asList(
                "小红：晚上吃火锅吗？",
                "阿强：你上线了吗？",
                "公司群：下午三点开会",
                "老王：收到文件请回复"
        ));
    }

    public void loadContacts() {
        listData.setValue(Arrays.asList(
                "小明",
                "小红",
                "阿强",
                "程序猿老李",
                "班长",
                "刘德华（在线）"
        ));
    }
}
