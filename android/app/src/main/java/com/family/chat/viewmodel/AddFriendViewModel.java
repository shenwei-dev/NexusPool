package com.family.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.family.chat.common.ApiResponse;
import com.family.chat.vo.QueryFriendRequestListVO;
import com.family.chat.repository.FriendRequestRepository;
import java.util.ArrayList;
import java.util.List;

public class AddFriendViewModel extends ViewModel {

    private final FriendRequestRepository friendRequestRepository;

    private final MutableLiveData<List<QueryFriendRequestListVO>> friendRequests = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // 标记哪一条目被同意成功
    private final MutableLiveData<Integer> acceptSuccessPosition = new MutableLiveData<>();

    public AddFriendViewModel() {
        this.friendRequestRepository = new FriendRequestRepository();
    }

    public LiveData<List<QueryFriendRequestListVO>> getFriendRequests() {
        return friendRequests;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Integer> getAcceptSuccessPosition() {
        return acceptSuccessPosition;
    }

    /**
     * 1. 异步加载历史好友申请列表 (HTTP GET)
     */
    public void loadFriendRequests() {
        isLoading.postValue(true);
        friendRequestRepository.getFriendRequestList(new FriendRequestRepository.FriendRequestCallback<ApiResponse<List<QueryFriendRequestListVO>>>() {
            @Override
            public void onSuccess(ApiResponse<List<QueryFriendRequestListVO>> response) {
                isLoading.postValue(false);
                if (response != null && response.getCode() == 1000 && response.getData() != null) {
                    friendRequests.setValue(response.getData());
                } else {
                    errorMessage.setValue("获取申请列表失败");
                }
            }

            @Override
            public void onFailure(String error) {
                isLoading.postValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    /**
     * 2. 异步同意接受好友申请 (HTTP POST)
     */
    public void acceptFriendRequest(Long requestId, int position) {
        isLoading.postValue(true);
        friendRequestRepository.acceptFriendRequest(requestId, new FriendRequestRepository.FriendRequestCallback<ApiResponse<Void>>() {
            @Override
            public void onSuccess(ApiResponse<Void> response) {
                isLoading.postValue(false);
                if (response != null) {
                    // 通知 Activity 修改成功，局部更新数据状态
                    acceptSuccessPosition.setValue(position);
                } else {
                    errorMessage.setValue(response != null ? response.getMessage() : "接受申请失败");
                }
            }

            @Override
            public void onFailure(String error) {
                isLoading.postValue(false);
                errorMessage.setValue(error);
            }
        });
    }
}
