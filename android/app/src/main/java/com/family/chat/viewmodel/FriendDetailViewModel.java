package com.family.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.family.chat.common.ApiResponse;
import com.family.chat.util.AvatarManager;
import com.family.chat.util.FriendDetailCache;
import com.family.chat.vo.FriendDetailVO;
import com.family.chat.repository.FriendRepository;

public class
FriendDetailViewModel extends ViewModel {

    private final MutableLiveData<FriendDetailVO> friendDetail = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final FriendRepository friendRepository = new FriendRepository();

    public LiveData<FriendDetailVO> getFriendDetail() {
        return friendDetail;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * 根据好友 ID 查询好友详细信息 (cache-aside 策略)
     *
     * <p>1. 优先渲染本地缓存, 实现页面秒开与离线可用;
     * 2. 再发起网络请求, 成功后覆盖缓存与界面;
     * 3. 网络失败且无缓存兜底时才向上抛出错误。
     *
     * @param friendId 好友ID (Long类型，对应您的API接口定义)
     */
    public void loadFriendDetail(Long friendId) {
        if (friendId == null) {
            errorMessage.setValue("好友ID不能为空");
            return;
        }

        // 1. 缓存优先: 命中则立即渲染, 离线场景同样可用
        FriendDetailVO cached = FriendDetailCache.load(friendId);
        if (cached != null) {
            AvatarManager.put(cached.getFriendId(), cached.getAvatarUrl());
            friendDetail.setValue(cached);
        }

        // 2. 网络刷新: 成功后覆盖缓存与界面
        isLoading.setValue(true);
        friendRepository.getFriendDetail(friendId, new FriendRepository.FriendCallback<ApiResponse<FriendDetailVO>>() {
            @Override
            public void onSuccess(ApiResponse<FriendDetailVO> response) {
                isLoading.setValue(false);
                FriendDetailVO data = response.getData();
                if (data == null) {
                    return;
                }
                FriendDetailCache.save(data);
                AvatarManager.put(data.getFriendId(), data.getAvatarUrl());
                friendDetail.setValue(data);
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                // 已有缓存兜底时静默降级, 避免离线场景误报错误
                if (friendDetail.getValue() == null) {
                    errorMessage.setValue(error != null ? error : "请求发生异常");
                }
            }
        });
    }
}