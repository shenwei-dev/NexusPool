package com.family.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.family.chat.common.ApiResponse;
import com.family.chat.util.AvatarManager;
import com.family.chat.util.FriendListCache;
import com.family.chat.vo.FriendVO;
import com.family.chat.repository.FriendRepository;
import java.util.List;

public class FriendsViewModel extends ViewModel {

    private final FriendRepository repository;

    /** 好友列表内存缓存, 进程内切换页面免磁盘 IO */
    private List<FriendVO> memoryCache;

    private final MutableLiveData<List<FriendVO>> friendList = new MutableLiveData<>();

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public FriendsViewModel() {
        repository = new FriendRepository();
    }

    public LiveData<List<FriendVO>> getFriendList(){
        return friendList;
    }

    public LiveData<String> getErrorMessage(){
        return errorMessage;
    }

    public void loadFriends(){
        // 缓存优先: 内存缓存 -> 本地磁盘缓存, 先渲染秒开列表
        if (memoryCache == null) {
            memoryCache = FriendListCache.load();
        }
        if (!memoryCache.isEmpty()) {
            fillAvatarCache(memoryCache);
            friendList.postValue(memoryCache);
        }

        // 网络刷新: 成功后覆盖缓存与界面
        repository.getFriendList(
                new FriendRepository.FriendCallback<ApiResponse<List<FriendVO>>>() {
                    @Override
                    public void onSuccess(ApiResponse<List<FriendVO>> response) {
                        List<FriendVO> data = response.getData();
                        if (data == null) {
                            return;
                        }
                        memoryCache = data;
                        FriendListCache.save(data);
                        fillAvatarCache(data);
                        friendList.postValue(data);
                    }
                    @Override
                    public void onFailure(String error){
                        // 已有缓存兜底时静默降级, 避免离线场景误报错误
                        if (memoryCache == null || memoryCache.isEmpty()) {
                            errorMessage.postValue(error);
                        }
                    }
                }
        );
    }

    /** 好友列表携带的头像写入全局缓存, 供聊天/通话界面按 userId 读取 */
    private void fillAvatarCache(List<FriendVO> friends) {
        for (FriendVO friend : friends) {
            AvatarManager.put(friend.getFriendId(), friend.getAvatarUrl());
        }
    }

}
